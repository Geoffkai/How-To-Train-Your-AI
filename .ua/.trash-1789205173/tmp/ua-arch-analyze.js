#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');

function fail(msg) {
  console.error('ERROR: ' + msg);
  process.exit(1);
}

const inputPath = process.argv[2];
const outputPath = process.argv[3];
if (!inputPath || !outputPath) {
  fail('Usage: node ua-arch-analyze.js <input.json> <output.json>');
}

let raw;
try {
  raw = fs.readFileSync(inputPath, 'utf8');
} catch (e) {
  fail('Cannot read input file: ' + e.message);
}

let data;
try {
  data = JSON.parse(raw);
} catch (e) {
  fail('Invalid JSON input: ' + e.message);
}

const fileNodes = data.fileNodes || [];
const importEdges = data.importEdges || [];
const allEdges = data.allEdges || [];

// ---------- A. Directory Grouping ----------
function posix(p) {
  return String(p).replace(/\\/g, '/');
}

const paths = fileNodes.map(n => posix(n.filePath || n.name || ''));

function commonPrefix(strs) {
  if (strs.length === 0) return '';
  let prefix = strs[0];
  for (let i = 1; i < strs.length; i++) {
    const s = strs[i];
    let j = 0;
    while (j < prefix.length && j < s.length && prefix[j] === s[j]) j++;
    prefix = prefix.slice(0, j);
  }
  // trim back to last '/'
  const idx = prefix.lastIndexOf('/');
  return idx >= 0 ? prefix.slice(0, idx + 1) : '';
}

const prefix = commonPrefix(paths);

function extPattern(fname) {
  if (/\.test\.|\.spec\./.test(fname)) return 'test';
  if (/\.config\./.test(fname)) return 'config';
  const m = fname.match(/\.([a-zA-Z0-9]+)$/);
  return m ? m[1] : 'other';
}

const directoryGroups = {};
const hasSubdirs = fileNodes.some(n => {
  const p = posix(n.filePath || '');
  const rest = prefix && p.startsWith(prefix) ? p.slice(prefix.length) : p;
  return rest.includes('/');
});

for (const node of fileNodes) {
  const p = posix(node.filePath || node.name || '');
  let group;
  if (hasSubdirs) {
    const rest = prefix && p.startsWith(prefix) ? p.slice(prefix.length) : p;
    if (rest.includes('/')) {
      group = rest.split('/')[0];
    } else {
      // file directly under prefix (or root) with no further subdir
      const segs = p.split('/');
      group = segs.length > 1 ? segs[segs.length - 2] : (path.dirname(p) === '.' ? 'root' : path.dirname(p));
      if (!group || group === '.') group = 'root';
    }
  } else {
    group = extPattern(node.name || p);
  }
  if (!directoryGroups[group]) directoryGroups[group] = [];
  directoryGroups[group].push(node.id);
}

// ---------- B. Node Type Grouping ----------
const nodeTypeGroups = {};
for (const node of fileNodes) {
  const t = node.type || 'file';
  if (!nodeTypeGroups[t]) nodeTypeGroups[t] = [];
  nodeTypeGroups[t].push(node.id);
}

// ---------- C. Import Adjacency Matrix ----------
const fileFanOut = {};
const fileFanIn = {};
const idToNode = {};
for (const n of fileNodes) idToNode[n.id] = n;

for (const e of importEdges) {
  if (!idToNode[e.source] || !idToNode[e.target]) continue;
  fileFanOut[e.source] = (fileFanOut[e.source] || 0) + 1;
  fileFanIn[e.target] = (fileFanIn[e.target] || 0) + 1;
}

// id -> group
const idToGroup = {};
for (const g of Object.keys(directoryGroups)) {
  for (const id of directoryGroups[g]) idToGroup[id] = g;
}

const groupImportsFrom = {}; // group -> Set(group)
const groupImportedBy = {};
for (const g of Object.keys(directoryGroups)) {
  groupImportsFrom[g] = new Set();
  groupImportedBy[g] = new Set();
}
for (const e of importEdges) {
  const sg = idToGroup[e.source];
  const tg = idToGroup[e.target];
  if (!sg || !tg || sg === tg) continue;
  groupImportsFrom[sg].add(tg);
  groupImportedBy[tg].add(sg);
}

// ---------- D. Cross-Category Dependency Analysis ----------
const crossCategoryMap = {};
for (const e of allEdges) {
  const sNode = idToNode[e.source];
  const tNode = idToNode[e.target];
  if (!sNode || !tNode) continue;
  const sType = sNode.type || 'file';
  const tType = tNode.type || 'file';
  if (sType === tType && sType === 'file') continue; // handled elsewhere, focus on cross-category
  const key = sType + '->' + tType + '->' + e.type;
  crossCategoryMap[key] = (crossCategoryMap[key] || 0) + 1;
}
const crossCategoryEdges = Object.keys(crossCategoryMap).map(key => {
  const [fromType, toType, edgeType] = key.split('->');
  return { fromType, toType, edgeType, count: crossCategoryMap[key] };
});

// ---------- E. Inter-Group Import Frequency ----------
const interGroupMap = {};
for (const e of importEdges) {
  const sg = idToGroup[e.source];
  const tg = idToGroup[e.target];
  if (!sg || !tg || sg === tg) continue;
  const key = sg + '=>' + tg;
  interGroupMap[key] = (interGroupMap[key] || 0) + 1;
}
const interGroupImports = Object.keys(interGroupMap).map(key => {
  const [from, to] = key.split('=>');
  return { from, to, count: interGroupMap[key] };
});

// ---------- F. Intra-Group Import Density ----------
const intraGroupDensity = {};
for (const g of Object.keys(directoryGroups)) {
  let internalEdges = 0;
  let totalEdges = 0;
  for (const e of importEdges) {
    const sg = idToGroup[e.source];
    const tg = idToGroup[e.target];
    if (sg !== g && tg !== g) continue;
    totalEdges++;
    if (sg === g && tg === g) internalEdges++;
  }
  intraGroupDensity[g] = {
    internalEdges,
    totalEdges,
    density: totalEdges > 0 ? +(internalEdges / totalEdges).toFixed(3) : 0
  };
}

// ---------- G. Directory Pattern Matching ----------
const DIR_PATTERNS = [
  [['routes', 'api', 'controllers', 'endpoints', 'handlers', 'controller', 'routers', 'blueprints'], 'api'],
  [['services', 'core', 'lib', 'domain', 'logic', 'internal', 'signals', 'composables', 'mailers', 'jobs', 'channels'], 'service'],
  [['models', 'db', 'data', 'persistence', 'repository', 'entities', 'migrations', 'entity', 'sql', 'database', 'schema'], 'data'],
  [['components', 'views', 'pages', 'ui', 'layouts', 'screens'], 'ui'],
  [['middleware', 'plugins', 'interceptors', 'guards'], 'middleware'],
  [['utils', 'helpers', 'common', 'shared', 'tools', 'templatetags', 'pkg'], 'utility'],
  [['config', 'constants', 'env', 'settings', 'management', 'commands'], 'config'],
  [['__tests__', 'test', 'tests', 'spec', 'specs'], 'test'],
  [['types', 'interfaces', 'schemas', 'contracts', 'dtos', 'dto', 'request', 'response'], 'types'],
  [['hooks'], 'hooks'],
  [['store', 'state', 'reducers', 'actions', 'slices'], 'state'],
  [['assets', 'static', 'public'], 'assets'],
  [['cmd'], 'entry'],
  [['bin'], 'entry'],
  [['docs', 'documentation', 'wiki'], 'documentation'],
  [['deploy', 'deployment', 'infra', 'infrastructure', 'k8s', 'kubernetes', 'helm', 'charts', 'terraform', 'tf', 'docker'], 'infrastructure'],
  [['.github', '.gitlab', '.circleci'], 'ci-cd'],
  [['engine'], 'service'],
  [['model'], 'data'],
  [['gui'], 'ui'],
];

function matchDirPattern(dirName) {
  const lower = dirName.toLowerCase();
  for (const [names, label] of DIR_PATTERNS) {
    if (names.includes(lower)) return label;
  }
  return null;
}

const patternMatches = {};
for (const g of Object.keys(directoryGroups)) {
  const m = matchDirPattern(g);
  if (m) patternMatches[g] = m;
}

// ---------- H. Deployment Topology Detection ----------
const infraFiles = [];
let hasDockerfile = false, hasCompose = false, hasK8s = false, hasTerraform = false, hasCI = false;
for (const node of fileNodes) {
  const p = posix(node.filePath || '');
  const base = path.basename(p);
  if (/^Dockerfile/.test(base)) { hasDockerfile = true; infraFiles.push(p); }
  if (/^docker-compose/.test(base)) { hasCompose = true; infraFiles.push(p); }
  if (/\.tf$|\.tfvars$/.test(base)) { hasTerraform = true; infraFiles.push(p); }
  if (/^\.github\/workflows\//.test(p) || /\.gitlab-ci\.yml$/.test(base) || base === 'Jenkinsfile') { hasCI = true; infraFiles.push(p); }
  if (/k8s|kubernetes|helm/.test(p.toLowerCase())) { hasK8s = true; infraFiles.push(p); }
}
const deploymentTopology = {
  hasDockerfile, hasCompose, hasK8s, hasTerraform, hasCI,
  infraFiles: Array.from(new Set(infraFiles))
};

// ---------- I. Data Pipeline Detection ----------
const schemaFiles = [];
const migrationFiles = [];
const dataModelFiles = [];
const apiHandlerFiles = [];
for (const node of fileNodes) {
  const p = posix(node.filePath || '');
  const tags = node.tags || [];
  if (/\.sql$|\.graphql$|\.gql$|\.proto$/.test(p) || node.type === 'schema' || node.type === 'table') schemaFiles.push(p);
  if (/migrations\//.test(p)) migrationFiles.push(p);
  if (tags.includes('data-model') || /model|entity/.test(p.toLowerCase())) dataModelFiles.push(p);
  if (tags.includes('api-handler') || /route|controller|endpoint|handler/.test(p.toLowerCase())) apiHandlerFiles.push(p);
}
const dataPipeline = { schemaFiles, migrationFiles, dataModelFiles, apiHandlerFiles };

// ---------- J. Documentation Coverage ----------
const docFiles = (nodeTypeGroups['document'] || []).map(id => posix(idToNode[id].filePath || ''));
const groupsWithDocsSet = new Set();
for (const g of Object.keys(directoryGroups)) {
  const hasReadme = docFiles.some(d => d.toLowerCase().includes(g.toLowerCase()));
  if (hasReadme) groupsWithDocsSet.add(g);
}
const totalGroups = Object.keys(directoryGroups).length;
const groupsWithDocs = groupsWithDocsSet.size;
const undocumentedGroups = Object.keys(directoryGroups).filter(g => !groupsWithDocsSet.has(g));
const docCoverage = {
  groupsWithDocs,
  totalGroups,
  coverageRatio: totalGroups > 0 ? +(groupsWithDocs / totalGroups).toFixed(3) : 0,
  undocumentedGroups
};

// ---------- K. Dependency Direction ----------
const dependencyDirection = [];
const seenPairs = new Set();
for (const { from, to, count } of interGroupImports) {
  const reverseKey = to + '=>' + from;
  const reverseCount = interGroupMap[reverseKey] || 0;
  const pairKey = [from, to].sort().join('|');
  if (seenPairs.has(pairKey)) continue;
  seenPairs.add(pairKey);
  if (count > reverseCount) {
    dependencyDirection.push({ dependent: from, dependsOn: to });
  } else if (reverseCount > count) {
    dependencyDirection.push({ dependent: to, dependsOn: from });
  }
}

// ---------- File Stats ----------
const filesPerGroup = {};
for (const g of Object.keys(directoryGroups)) filesPerGroup[g] = directoryGroups[g].length;
const nodeTypeCounts = {};
for (const t of Object.keys(nodeTypeGroups)) nodeTypeCounts[t] = nodeTypeGroups[t].length;

const result = {
  scriptCompleted: true,
  directoryGroups,
  nodeTypeGroups,
  crossCategoryEdges,
  interGroupImports,
  intraGroupDensity,
  patternMatches,
  deploymentTopology,
  dataPipeline,
  docCoverage,
  dependencyDirection,
  fileStats: {
    totalFileNodes: fileNodes.length,
    filesPerGroup,
    nodeTypeCounts
  },
  fileFanIn,
  fileFanOut
};

try {
  fs.writeFileSync(outputPath, JSON.stringify(result, null, 2), 'utf8');
} catch (e) {
  fail('Cannot write output file: ' + e.message);
}

console.log('Analysis complete. Wrote results to ' + outputPath);
process.exit(0);
