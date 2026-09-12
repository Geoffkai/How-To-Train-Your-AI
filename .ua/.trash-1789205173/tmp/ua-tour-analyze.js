#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');

function fail(msg) {
  process.stderr.write('Error: ' + msg + '\n');
  process.exit(1);
}

const inputPath = process.argv[2];
const outputPath = process.argv[3];

if (!inputPath || !outputPath) {
  fail('Usage: node ua-tour-analyze.js <input.json> <output.json>');
}

let raw;
try {
  raw = fs.readFileSync(inputPath, 'utf8');
} catch (e) {
  fail('Could not read input file: ' + e.message);
}

let data;
try {
  data = JSON.parse(raw);
} catch (e) {
  fail('Invalid JSON in input file: ' + e.message);
}

const nodes = Array.isArray(data.nodes) ? data.nodes : [];
const edges = Array.isArray(data.edges) ? data.edges : [];
const layers = Array.isArray(data.layers) ? data.layers : [];

try {
  const nodeById = new Map();
  for (const n of nodes) nodeById.set(n.id, n);

  // Fan-in / Fan-out
  const fanIn = new Map();
  const fanOut = new Map();
  for (const n of nodes) {
    fanIn.set(n.id, 0);
    fanOut.set(n.id, 0);
  }
  for (const e of edges) {
    if (fanOut.has(e.source)) fanOut.set(e.source, fanOut.get(e.source) + 1);
    if (fanIn.has(e.target)) fanIn.set(e.target, fanIn.get(e.target) + 1);
  }

  const fanInRanking = [...fanIn.entries()]
    .map(([id, count]) => ({ id, fanIn: count, name: nodeById.get(id) ? nodeById.get(id).name : id }))
    .sort((a, b) => b.fanIn - a.fanIn)
    .slice(0, 20);

  const fanOutRanking = [...fanOut.entries()]
    .map(([id, count]) => ({ id, fanOut: count, name: nodeById.get(id) ? nodeById.get(id).name : id }))
    .sort((a, b) => b.fanOut - a.fanOut)
    .slice(0, 20);

  // Percentile thresholds for entry point scoring
  const fanOutValues = [...fanOut.values()].sort((a, b) => a - b);
  const fanInValues = [...fanIn.values()].sort((a, b) => a - b);
  function percentileThreshold(sortedValues, percentile) {
    if (sortedValues.length === 0) return 0;
    const idx = Math.floor(sortedValues.length * percentile);
    return sortedValues[Math.min(idx, sortedValues.length - 1)];
  }
  const fanOutTop10Threshold = percentileThreshold(fanOutValues, 0.9);
  const fanInBottom25Threshold = percentileThreshold(fanInValues, 0.25);

  const ENTRY_FILENAMES = new Set([
    'index.ts', 'index.js', 'main.ts', 'main.js', 'app.ts', 'app.js', 'server.ts', 'server.js',
    'mod.rs', 'main.go', 'main.py', 'main.rs', 'manage.py', 'app.py', 'wsgi.py', 'asgi.py',
    'run.py', '__main__.py', 'Application.java', 'Main.java', 'Program.cs', 'config.ru',
    'index.php', 'App.swift', 'Application.kt', 'main.cpp', 'main.c'
  ]);

  function pathDepth(filePath) {
    if (!filePath) return 99;
    const normalized = filePath.replace(/\\/g, '/');
    return normalized.split('/').filter(Boolean).length - 1;
  }

  const entryPointCandidates = [];
  for (const n of nodes) {
    let score = 0;
    const baseName = n.name || (n.filePath ? path.basename(n.filePath) : '');
    if (n.type === 'document') {
      const depth = pathDepth(n.filePath);
      if (baseName === 'README.md' && depth === 0) {
        score += 5;
      } else if (/\.md$/i.test(baseName) && depth === 0) {
        score += 2;
      }
    } else if (n.type === 'file') {
      if (ENTRY_FILENAMES.has(baseName)) score += 3;
      const depth = pathDepth(n.filePath);
      if (depth <= 1) score += 1;
      if (fanOut.get(n.id) >= fanOutTop10Threshold && fanOut.get(n.id) > 0) score += 1;
      if (fanIn.get(n.id) <= fanInBottom25Threshold) score += 1;
    }
    if (score > 0) {
      entryPointCandidates.push({ id: n.id, score, name: n.name, summary: n.summary });
    }
  }
  entryPointCandidates.sort((a, b) => b.score - a.score);
  const topEntryPointCandidates = entryPointCandidates.slice(0, 5);

  // BFS from top code entry point (skip documentation nodes)
  const topCodeEntry = entryPointCandidates.find(c => {
    const n = nodeById.get(c.id);
    return n && n.type !== 'document';
  });

  const traversalEdgeTypes = new Set(['imports', 'calls']);
  const adjacency = new Map();
  for (const n of nodes) adjacency.set(n.id, []);
  for (const e of edges) {
    if (traversalEdgeTypes.has(e.type) && adjacency.has(e.source)) {
      adjacency.get(e.source).push(e.target);
    }
  }

  const bfsTraversal = { startNode: null, order: [], depthMap: {}, byDepth: {} };
  if (topCodeEntry) {
    const start = topCodeEntry.id;
    bfsTraversal.startNode = start;
    const visited = new Set([start]);
    const queue = [[start, 0]];
    let head = 0;
    while (head < queue.length) {
      const [nodeId, depth] = queue[head++];
      bfsTraversal.order.push(nodeId);
      bfsTraversal.depthMap[nodeId] = depth;
      if (!bfsTraversal.byDepth[depth]) bfsTraversal.byDepth[depth] = [];
      bfsTraversal.byDepth[depth].push(nodeId);
      const neighbors = adjacency.get(nodeId) || [];
      for (const neighbor of neighbors) {
        if (!visited.has(neighbor)) {
          visited.add(neighbor);
          queue.push([neighbor, depth + 1]);
        }
      }
    }
  }

  // Non-code file inventory
  const nonCodeFiles = { documentation: [], infrastructure: [], data: [], config: [] };
  for (const n of nodes) {
    const entry = { id: n.id, name: n.name, type: n.type, summary: n.summary };
    if (n.type === 'document') nonCodeFiles.documentation.push(entry);
    else if (n.type === 'service' || n.type === 'pipeline' || n.type === 'resource') nonCodeFiles.infrastructure.push(entry);
    else if (n.type === 'table' || n.type === 'schema' || n.type === 'endpoint') nonCodeFiles.data.push(entry);
    else if (n.type === 'config') nonCodeFiles.config.push(entry);
  }

  // Tightly coupled clusters: bidirectional relationships, then expand
  const edgeSet = new Set(edges.map(e => e.source + '|' + e.target + '|' + e.type));
  const undirectedPairCount = new Map();
  function pairKey(a, b) {
    return a < b ? a + '::' + b : b + '::' + a;
  }
  for (const e of edges) {
    const key = pairKey(e.source, e.target);
    undirectedPairCount.set(key, (undirectedPairCount.get(key) || 0) + 1);
  }

  const bidirectionalPairs = [];
  const relTypes = ['imports', 'calls'];
  for (const e of edges) {
    if (!relTypes.includes(e.type)) continue;
    const reverseExists = relTypes.some(t => edgeSet.has(e.target + '|' + e.source + '|' + t));
    if (reverseExists) {
      bidirectionalPairs.push([e.source, e.target]);
    }
  }

  // Union-find to group bidirectional pairs into clusters
  const parent = new Map();
  function find(x) {
    if (!parent.has(x)) parent.set(x, x);
    if (parent.get(x) !== x) parent.set(x, find(parent.get(x)));
    return parent.get(x);
  }
  function union(a, b) {
    const ra = find(a), rb = find(b);
    if (ra !== rb) parent.set(ra, rb);
  }
  for (const [a, b] of bidirectionalPairs) {
    union(a, b);
  }

  const clusterGroups = new Map();
  for (const [a] of bidirectionalPairs) {
    const root = find(a);
    if (!clusterGroups.has(root)) clusterGroups.set(root, new Set());
    clusterGroups.get(root).add(a);
  }
  for (const [, b] of bidirectionalPairs) {
    const root = find(b);
    if (!clusterGroups.has(root)) clusterGroups.set(root, new Set());
    clusterGroups.get(root).add(b);
  }

  // Expand: add nodes connecting to 2+ existing cluster members
  const allEdgeNeighbors = new Map();
  for (const n of nodes) allEdgeNeighbors.set(n.id, new Set());
  for (const e of edges) {
    if (allEdgeNeighbors.has(e.source)) allEdgeNeighbors.get(e.source).add(e.target);
    if (allEdgeNeighbors.has(e.target)) allEdgeNeighbors.get(e.target).add(e.source);
  }

  const clusters = [];
  for (const [, memberSet] of clusterGroups) {
    let members = new Set(memberSet);
    let changed = true;
    while (changed && members.size < 5) {
      changed = false;
      for (const n of nodes) {
        if (members.has(n.id)) continue;
        const neighbors = allEdgeNeighbors.get(n.id) || new Set();
        let connections = 0;
        for (const m of members) if (neighbors.has(m)) connections++;
        if (connections >= 2) {
          members.add(n.id);
          changed = true;
          if (members.size >= 5) break;
        }
      }
    }
    const memberArr = [...members].slice(0, 5);
    let edgeCount = 0;
    for (const e of edges) {
      if (memberArr.includes(e.source) && memberArr.includes(e.target)) edgeCount++;
    }
    clusters.push({ nodes: memberArr, edgeCount });
  }
  clusters.sort((a, b) => b.edgeCount - a.edgeCount);
  const topClusters = clusters.slice(0, 10);

  // Layers
  const layersOut = { count: layers.length, list: layers.map(l => ({ id: l.id, name: l.name, description: l.description })) };

  // Node summary index
  const nodeSummaryIndex = {};
  for (const n of nodes) {
    nodeSummaryIndex[n.id] = { name: n.name, type: n.type, summary: n.summary };
  }

  const result = {
    scriptCompleted: true,
    entryPointCandidates: topEntryPointCandidates,
    fanInRanking,
    fanOutRanking,
    bfsTraversal,
    nonCodeFiles,
    clusters: topClusters,
    layers: layersOut,
    nodeSummaryIndex,
    totalNodes: nodes.length,
    totalEdges: edges.length
  };

  fs.writeFileSync(outputPath, JSON.stringify(result, null, 2), 'utf8');
  process.exit(0);
} catch (e) {
  fail(e.stack || e.message);
}
