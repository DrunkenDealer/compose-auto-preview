const DENSITY = DATA.density || 2;
const screens = DATA.screens;
const byId = new Map(screens.map(s => [s.id, s]));
const $ = id => document.getElementById(id);
const page = $("page");
const clamp = (v, lo, hi) => Math.min(hi, Math.max(lo, v));
const store = {
  get(key) { try { return localStorage.getItem("autopreview." + key); } catch { return null; } },
  set(key, value) { try { localStorage.setItem("autopreview." + key, value); } catch {} },
};

function el(tag, attrs = {}, ...children) {
  const node = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (v == null || v === false) continue;
    if (k.startsWith("on")) node.addEventListener(k.slice(2), v); else node.setAttribute(k, v === true ? "" : v);
  }
  node.append(...children.flat().filter(c => c != null && c !== false));
  return node;
}
const imageUrl = cell => IMAGES + "/" + cell.image.split("/").map(encodeURIComponent).join("/");
const screenUrl = (id, device) => "#/screen/" + encodeURIComponent(id) + (device ? "/" + encodeURIComponent(device) : "");
const deviceOf = (screen, name) => screen.devices.find(d => d.name === name);
const failures = screen => screen.cells.filter(c => c.error).length;
const plural = (n, word) => `${n} ${word}${n === 1 ? "" : "s"}`;

// ---- Navigation model -----------------------------------------------------------------------
// Screens sharing a `group` (the tabs of a bottom bar, a flow) reach each other without navigatesTo, so links between
// them are left out and the group is drawn as a box instead.
const groups = new Map();
screens.forEach(s => s.group && groups.set(s.group, [...(groups.get(s.group) ?? []), s.id]));
const siblings = id => (groups.get(byId.get(id).group) ?? []).filter(t => t !== id);
const sameGroup = (a, b) => !!byId.get(a).group && byId.get(a).group === byId.get(b).group;
const edges = screens.flatMap(s => [...new Set(s.navigatesTo)].filter(t => byId.has(t) && t !== s.id && !sameGroup(s.id, t))
  .map(t => ({ source: s.id, target: t })));
const outgoing = id => edges.filter(e => e.source === id).map(e => e.target);
const incoming = id => edges.filter(e => e.target === id).map(e => e.source);
const unknownTargets = screens.flatMap(s => s.navigatesTo.filter(t => !byId.has(t)).map(t => `${s.id} → ${t}`));
const entry = screens.find(s => s.entryPoint);

// Hops from the entry point (BFS over navigatesTo). Without one, screens nobody navigates to act as roots.
// A group is entered as a whole: its screens share the hop count of the first one reached.
const depth = new Map();
{
  let roots = entry ? [entry.id] : screens.filter(s => ![s.id, ...siblings(s.id)].some(id => incoming(id).length)).map(s => s.id);
  if (!roots.length && screens.length) roots = [screens[0].id];
  const queue = [];
  const reach = (id, d) => [id, ...siblings(id)].forEach(t => { if (!depth.has(t)) { depth.set(t, d); queue.push(t); } });
  roots.forEach(id => reach(id, 0));
  while (queue.length) {
    const id = queue.shift();
    for (const t of outgoing(id)) if (!depth.has(t)) reach(t, depth.get(id) + 1);
  }
}
const maxDepth = Math.max(0, ...depth.values());
const hops = id => depth.has(id) ? depth.get(id) : maxDepth + 1;
const isUnreachable = id => !depth.has(id);
const ordered = [...screens].sort((a, b) => hops(a.id) - hops(b.id) || (a.group ?? "").localeCompare(b.group ?? "")
  || a.id.localeCompare(b.id));

function thumbnail(screen) {
  const ok = screen.cells.filter(c => !c.error);
  const cell = ok.find(c => c.device === screen.devices[0].name && c.theme === "Light") || ok[0];
  return cell && { cell, device: deviceOf(screen, cell.device) };
}
function thumbImg(screen, attrs = {}) {
  const t = thumbnail(screen);
  if (!t) return el("span", { class: "ph" });
  return el("img", { src: imageUrl(t.cell), alt: "", decoding: "async", draggable: "false",
    width: t.device.widthDp * DENSITY, height: t.device.heightDp * DENSITY, ...attrs });
}
const THUMB_BEZEL = 2.5;
const frameSize = device => { const f = deviceFrame(device); return `--ar:${f.width / f.height};--fw:${f.width}px`; };
function framedThumb(screen) {
  const t = thumbnail(screen);
  return t ? framed(t.device, thumbImg(screen), THUMB_BEZEL) : thumbImg(screen);
}

// ---- Header -------------------------------------------------------------------------------
{
  const images = screens.reduce((n, s) => n + s.cells.length, 0);
  const failed = screens.reduce((n, s) => n + failures(s), 0);
  $("stats").append(`${plural(screens.length, "screen")} · ${plural(images, "image")}`,
    failed ? el("span", { class: "bad" }, ` · ${failed} failed`) : "");

  const saved = store.get("theme");
  if (saved) document.documentElement.dataset.theme = saved;
  $("theme-toggle").addEventListener("click", () => {
    const current = document.documentElement.dataset.theme
      || (matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
    const next = current === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = next;
    store.set("theme", next);
  });
}

function notices() {
  const list = [];
  if (!screens.length) return [el("div", { class: "notice" }, "No screens yet. Annotate a preview with ", el("code", {}, "@AutoPreview"), ".")];
  if (!entry) list.push(el("div", { class: "notice" }, "No entry point. Add ", el("code", {}, "entryPoint = true"),
    " to your start screen's @AutoPreview to lay the graph out from it."));
  if (unknownTargets.length) list.push(el("div", { class: "notice" }, "Unknown navigatesTo targets: " + unknownTargets.join(", ")));
  return list;
}

// ---- Graph --------------------------------------------------------------------------------
// Layered layout after Sugiyama et al., as in Graphviz dot and ELK Layered: screens sit in columns by hop count from the
// entry point, links pointing backwards are reversed for layout, long links get waypoints in the columns they cross,
// barycenter sweeps reduce crossings, and links run between columns as curves leaving right and entering left.
const graph = (() => {
  const viewport = $("viewport"), world = $("world"), svg = $("edges");
  const COL_GAP = 160, ROW_GAP = 40, WAYPOINT_H = 12, FLAT_BULGE = 56, LABEL_HEIGHT = 32, TAG_HEIGHT = 34;
  const GROUP_PAD = 20, GROUP_LABEL = 30;
  const cam = { x: 0, y: 0, k: 1 };
  let cameraTouched = false, gesture = null;

  const rootId = entry ? entry.id : null;
  const nodes = ordered.map(screen => {
    const t = thumbnail(screen), f = t && deviceFrame(t.device, THUMB_BEZEL);
    const ratio = f ? f.width / f.height : 0.5;
    let h = screen.id === rootId ? 180 : 116 + 14 * Math.min(incoming(screen.id).length, 3);
    let w = h * ratio;
    if (w > 240) { w = 240; h = w / ratio; }
    const tag = screen.id === rootId ? TAG_HEIGHT : 0, full = h + LABEL_HEIGHT + tag;
    // off: from the box centre to the thumbnail centre, where links attach.
    return { id: screen.id, screen, group: screen.group, rank: hops(screen.id), tw: w, th: h, w: Math.max(w, 80), h: full,
      off: tag + h / 2 - full / 2, x: 0, y: 0, prev: [], next: [] };
  });
  const nodeById = new Map(nodes.map(n => [n.id, n]));
  const anchor = v => v.y + v.off;
  const thumbTop = n => n.y - n.h / 2 + (n.id === rootId ? TAG_HEIGHT : 0);

  // A two-way pair becomes one link with arrows on both ends. Every link is laid out towards higher ranks.
  const pairs = new Set(edges.map(e => e.source + "\n" + e.target));
  const links = [];
  for (const e of edges) {
    const both = pairs.has(e.target + "\n" + e.source);
    if (both && e.source > e.target) continue;
    let s = nodeById.get(e.source), t = nodeById.get(e.target);
    const flip = t.rank < s.rank;
    if (flip) [s, t] = [t, s];
    links.push({ source: e.source, target: e.target, s, t, both, flat: s.rank === t.rank, via: [],
      back: flip && !both, arrowStart: both || flip, arrowEnd: both || !flip });
  }

  const layers = [];
  const place = v => (layers[v.rank] ??= []).push(v);
  nodes.forEach(place);
  for (const l of links) {
    if (l.flat) continue;
    for (let r = l.s.rank + 1; r < l.t.rank; r++) {
      const d = { rank: r, waypoint: true, w: 0, h: WAYPOINT_H, off: 0, x: 0, y: 0, prev: [], next: [] };
      l.via.push(d);
      place(d);
    }
    const chain = [l.s, ...l.via, l.t];
    for (let i = 1; i < chain.length; i++) { chain[i - 1].next.push(chain[i]); chain[i].prev.push(chain[i - 1]); }
  }

  const index = () => layers.forEach(layer => layer.forEach((v, i) => v.order = i));
  function crossings() {
    let c = 0;
    for (const layer of layers) {
      const segs = layer.flatMap(v => v.next.map(w => [v.order, w.order]));
      for (let i = 0; i < segs.length; i++)
        for (let j = i + 1; j < segs.length; j++) if ((segs[i][0] - segs[j][0]) * (segs[i][1] - segs[j][1]) < 0) c++;
    }
    return c;
  }
  const mean = (vs, f) => vs.reduce((a, v) => a + f(v), 0) / vs.length;
  // Orders a column by pos, keeping each group's screens next to each other at the group's mean position.
  function cluster(layer, pos) {
    const members = new Map();
    layer.forEach(v => v.group && members.set(v.group, [...(members.get(v.group) ?? []), v]));
    const at = v => v.group ? mean(members.get(v.group), pos) : pos(v);
    layer.sort((a, b) => at(a) - at(b) || (a.group ?? "").localeCompare(b.group ?? "") || pos(a) - pos(b))
      .forEach((v, k) => v.order = k);
  }
  index();
  layers.forEach(layer => cluster(layer, v => v.order));
  let best = crossings(), bestLayers = layers.map(l => [...l]);
  for (let i = 0; i < 12 && best; i++) {
    const down = i % 2 === 0;
    for (const layer of down ? layers.slice(1) : layers.slice(0, -1).reverse()) {
      for (const v of layer) { const adj = down ? v.prev : v.next; v.bc = adj.length ? mean(adj, w => w.order) : v.order; }
      cluster(layer, v => v.bc);
    }
    const c = crossings();
    if (c < best) { best = c; bestLayers = layers.map(l => [...l]); }
  }
  bestLayers.forEach((l, r) => layers[r] = l);
  index();

  // Columns left to right; within a column, keep the order and pull each vertex towards its neighbours.
  let colX = 0;
  for (const layer of layers) {
    const w = Math.max(...layer.map(v => v.w));
    layer.forEach(v => v.x = colX + w / 2);
    colX += w + COL_GAP;
  }
  // Leaving or entering a group also clears its box: the padding below it, the padding and label above it.
  const gap = (a, b) => (a.h + b.h) / 2 + (a.waypoint && b.waypoint ? WAYPOINT_H : ROW_GAP)
    + (a.group === b.group ? 0 : (a.group ? GROUP_PAD : 0) + (b.group ? GROUP_PAD + GROUP_LABEL : 0));
  const runs = layer => layer.reduce((rs, v, k) => {
    if (v.group && layer[k - 1]?.group === v.group) rs.at(-1).push(v); else if (v.group) rs.push([v]);
    return rs;
  }, []);
  function settle(layer, want) {
    layer.forEach((v, k) => v.y = want[k] - v.off);
    // A group stacks as one block around where its screens want to be, so its box stays tight.
    for (const run of runs(layer)) {
      const centre = mean(run, v => v.y);
      run.forEach((v, i) => v.y = i ? run[i - 1].y + gap(run[i - 1], v) : 0);
      const shift = centre - mean(run, v => v.y);
      run.forEach(v => v.y += shift);
    }
    for (let k = 1; k < layer.length; k++) layer[k].y = Math.max(layer[k].y, layer[k - 1].y + gap(layer[k - 1], layer[k]));
    const shift = mean(layer, v => want[v.order] - anchor(v));
    layer.forEach(v => v.y += shift);
  }
  for (const layer of layers) settle(layer, layer.map(() => 0));
  for (let i = 0; i < 10; i++) {
    for (const layer of i % 2 ? [...layers].reverse() : layers) {
      settle(layer, layer.map(v => {
        const adj = [...v.prev, ...v.next];
        return adj.length ? mean(adj, anchor) : anchor(v);
      }));
    }
  }
  for (const n of nodes) { n.lx = n.x; n.ly = n.y; }

  // Dragged positions survive reloads while the report shows the same screens and links.
  const layoutKey = JSON.stringify([nodes.map(n => [n.id, n.group]), edges.map(e => [e.source, e.target])]);
  const loadPositions = () => { try { const s = JSON.parse(store.get("positions")); return s?.key === layoutKey ? s.pos : {}; } catch { return {}; } };
  const savePositions = pos => store.set("positions", JSON.stringify({ key: layoutKey, pos }));
  for (const [id, [x, y]] of Object.entries(loadPositions())) { const n = nodeById.get(id); if (n) { n.x = x; n.y = y; } }

  // Links attach to the thumbnail's sides, spread along them by the angle they head off at, so they never cross at a node.
  // A same-column link bulges only FLAT_BULGE sideways, so it turns up or down sooner than a link to the next column.
  // After a drag, a link keeps the waypoints still lying between its ends, so it goes on routing around the columns it crosses.
  const route = l => l.via.filter(d => (d.x - l.s.x) * (l.t.x - d.x) > 0);
  function attach() {
    const sides = new Map(nodes.map(n => [n, { left: [], right: [] }]));
    for (const l of links) {
      const via = route(l);
      const towards = (from, to, v = to) => Math.atan2(anchor(v) - anchor(from), Math.abs(v.x - from.x) || FLAT_BULGE);
      // A screen dragged past the other end flips the link to the facing sides instead of looping through both.
      l.rev = !l.flat && l.t.x < l.s.x;
      sides.get(l.s)[l.rev ? "left" : "right"].push({ l, end: 0, key: towards(l.s, l.t, via[0]) });
      sides.get(l.t)[l.flat || l.rev ? "right" : "left"].push({ l, end: 1, key: towards(l.t, l.s, via.at(-1)) });
    }
    for (const [n, { left, right }] of sides) {
      for (const [list, dir] of [[left, -1], [right, 1]]) {
        list.sort((a, b) => a.key - b.key).forEach((p, i) => {
          const arrow = p.end ? p.l.arrowEnd : p.l.arrowStart;
          const point = [n.x + dir * (n.tw / 2 + (arrow ? 5 : 2)), thumbTop(n) + n.th * (0.15 + 0.7 * (i + 0.5) / list.length)];
          if (p.end) p.l.p1 = point; else p.l.p0 = point;
        });
      }
    }
  }
  function edgePath(l) {
    const [x0, y0] = l.p0, [x1, y1] = l.p1;
    if (l.flat) { const bx = Math.max(x0, x1) + FLAT_BULGE; return `M${x0} ${y0}C${bx} ${y0} ${bx} ${y1} ${x1} ${y1}`; }
    const pts = [l.p0, ...route(l).map(d => [d.x, d.y]), l.p1];
    let d = `M${x0} ${y0}`;
    for (let i = 1; i < pts.length; i++) {
      const [ax, ay] = pts[i - 1], [bx, by] = pts[i], dx = Math.max(Math.abs(bx - ax) / 2, 40) * (l.rev ? -1 : 1);
      d += `C${ax + dx} ${ay} ${bx - dx} ${by} ${bx} ${by}`;
    }
    return d;
  }

  const SVG = "http://www.w3.org/2000/svg";
  for (const l of links) {
    l.g = document.createElementNS(SVG, "g");
    // A background-coloured casing under each link cuts the links beneath it, so crossings read as over/under.
    l.casing = document.createElementNS(SVG, "path");
    l.casing.setAttribute("class", "edge-casing");
    l.path = document.createElementNS(SVG, "path");
    l.path.setAttribute("class", ["edge", l.back && "back", l.arrowStart && "start", l.arrowEnd && "end"].filter(Boolean).join(" "));
    l.g.append(l.casing, l.path);
    svg.append(l.g);
  }
  // Group boxes sit under the links and screens, and follow their screens when dragged.
  const boxes = [...groups].map(([name, ids]) => {
    const label = el("span", { class: "group-label" }, name);
    const box = { nodes: ids.map(id => nodeById.get(id)), label, el: el("div", { class: "group", "aria-hidden": "true" }, label) };
    world.prepend(box.el);
    return box;
  });
  // Wide enough for its label, centred on its screens.
  function boxBounds(box) {
    const x0 = Math.min(...box.nodes.map(n => n.x - n.w / 2)), x1 = Math.max(...box.nodes.map(n => n.x + n.w / 2));
    const half = Math.max((x1 - x0) / 2 + GROUP_PAD, box.label.offsetWidth / 2 + GROUP_PAD), c = (x0 + x1) / 2;
    return { x0: c - half, x1: c + half, y0: Math.min(...box.nodes.map(n => n.y - n.h / 2)) - GROUP_PAD - GROUP_LABEL,
      y1: Math.max(...box.nodes.map(n => n.y + n.h / 2)) + GROUP_PAD };
  }
  for (const n of nodes) {
    const failed = failures(n.screen);
    const unreachable = rootId && isUnreachable(n.id);
    const label = [n.id, n.group && `in ${n.group}`, n.id === rootId && "entry point", unreachable && "unreachable from entry point",
      `${outgoing(n.id).length} outgoing`, `${incoming(n.id).length} incoming`, failed && `${failed} failed`].filter(Boolean).join(", ");
    n.el = el("button", {
      type: "button", class: "node" + (n.id === rootId ? " entry" : "") + (unreachable ? " unreachable" : ""),
      "data-id": n.id, "aria-label": label, style: `width:${n.w}px`,
      // Pointer clicks are handled by the gesture code below; this covers Enter/Space.
      onclick: e => { if (e.detail === 0) open(n.id); },
      onpointerenter: () => { if (!gesture) highlight(n.id); },
      onpointerleave: () => { if (!gesture) highlight(null); },
      onfocus: () => highlight(n.id),
      onblur: () => highlight(null),
    },
      n.id === rootId ? el("span", { class: "start", "aria-hidden": "true" }, el("i"), "Entry point") : "",
      el("span", { class: "thumb", style: `width:${n.tw}px;height:${n.th}px` },
        framedThumb(n.screen), failed ? el("span", { class: "fail", "aria-hidden": "true" }, failed) : ""),
      el("span", { class: "label" }, n.id),
    );
    world.append(n.el);
  }

  function draw() {
    for (const n of nodes) n.el.style.transform = `translate(${n.x - n.w / 2}px,${n.y - n.h / 2}px)`;
    for (const box of boxes) {
      const b = boxBounds(box);
      Object.assign(box.el.style, { transform: `translate(${b.x0}px,${b.y0}px)`, width: `${b.x1 - b.x0}px`, height: `${b.y1 - b.y0}px` });
    }
    attach();
    for (const l of links) { const d = edgePath(l); l.casing.setAttribute("d", d); l.path.setAttribute("d", d); }
  }
  function relayout() {
    for (const n of nodes) { n.x = n.lx; n.y = n.ly; }
    store.set("positions", "");
    draw();
    cameraTouched = false;
    fit();
  }
  function applyCamera() {
    world.style.transform = `translate(${cam.x}px,${cam.y}px) scale(${cam.k})`;
    viewport.style.backgroundSize = `${24 * cam.k}px ${24 * cam.k}px`;
    viewport.style.backgroundPosition = `${cam.x}px ${cam.y}px`;
    // Labels fade out when zoomed far out, like Obsidian.
    viewport.style.setProperty("--label-opacity", clamp((cam.k - 0.3) / 0.25, 0, 1));
  }
  function zoomAt(px, py, k) {
    k = clamp(k, 0.1, 3);
    cam.x = px - (px - cam.x) * k / cam.k;
    cam.y = py - (py - cam.y) * k / cam.k;
    cam.k = k;
    applyCamera();
  }
  function zoomBy(factor) {
    cameraTouched = true;
    zoomAt(viewport.clientWidth / 2, viewport.clientHeight / 2, cam.k * factor);
  }
  function fit() {
    const W = viewport.clientWidth, H = viewport.clientHeight;
    if (!W || !H || !nodes.length) return;
    let minX = Math.min(...nodes.map(n => n.x - n.w / 2)), maxX = Math.max(...nodes.map(n => n.x + n.w / 2));
    let minY = Math.min(...nodes.map(n => n.y - n.h / 2)), maxY = Math.max(...nodes.map(n => n.y + n.h / 2));
    for (const b of boxes.map(boxBounds)) {
      minX = Math.min(minX, b.x0); maxX = Math.max(maxX, b.x1); minY = Math.min(minY, b.y0); maxY = Math.max(maxY, b.y1);
    }
    const b = svg.getBBox();
    if (b.width || b.height) {
      minX = Math.min(minX, b.x); maxX = Math.max(maxX, b.x + b.width);
      minY = Math.min(minY, b.y); maxY = Math.max(maxY, b.y + b.height);
    }
    // Keep clear of the search bar on top and the zoom controls / legend at the bottom-right.
    const top = 72, right = 72, bottom = 64, pad = 24;
    cam.k = clamp(Math.min((W - pad - right) / (maxX - minX), (H - top - bottom) / (maxY - minY)), 0.1, 1.2);
    cam.x = pad + (W - pad - right) / 2 - (minX + maxX) / 2 * cam.k;
    cam.y = top + (H - top - bottom) / 2 - (minY + maxY) / 2 * cam.k;
    applyCamera();
  }

  // Hovering a screen shows where it leads in the accent colour and where it is reached from in the second edge colour.
  let highlighted = null;
  function highlight(id) {
    viewport.classList.toggle("focus", !!id);
    const near = id ? new Set([id, ...outgoing(id), ...incoming(id), ...siblings(id)]) : new Set();
    nodes.forEach(n => n.el.classList.toggle("hot", near.has(n.id)));
    for (const l of links) {
      const out = !!id && (l.source === id || l.both && l.target === id), into = !!id && !out && l.target === id;
      l.path.classList.toggle("out", out);
      l.path.classList.toggle("in", into);
      if (l.g.classList.toggle("hot", out || into) && id !== highlighted) svg.append(l.g);
    }
    highlighted = id;
  }

  const open = id => location.hash = screenUrl(id);

  // Pointer input: drag a node, pan the canvas, pinch or wheel to zoom.
  const pointers = new Map();
  const local = e => { const r = viewport.getBoundingClientRect(); return [e.clientX - r.left, e.clientY - r.top]; };
  const toWorld = ([x, y]) => [(x - cam.x) / cam.k, (y - cam.y) / cam.k];

  viewport.addEventListener("pointerdown", e => {
    if (e.pointerType === "mouse" && e.button !== 0) return;
    pointers.set(e.pointerId, local(e));
    // Keeps move/up events coming while the pointer leaves the canvas.
    try { viewport.setPointerCapture(e.pointerId); } catch {}
    if (pointers.size === 2) {
      const [a, b] = [...pointers.values()];
      gesture = { type: "pinch", d0: Math.hypot(a[0] - b[0], a[1] - b[1]) || 1, k0: cam.k,
        anchor: toWorld([(a[0] + b[0]) / 2, (a[1] + b[1]) / 2]) };
      return;
    }
    const nodeEl = e.target.closest(".node");
    const node = nodeEl && nodeById.get(nodeEl.dataset.id);
    const [wx, wy] = toWorld(local(e));
    gesture = { type: node ? "node" : "pan", node, start: local(e), cam0: { ...cam }, grab: node && [wx - node.x, wy - node.y], moved: false };
  });
  viewport.addEventListener("pointermove", e => {
    if (!pointers.has(e.pointerId) || !gesture) return;
    const p = local(e);
    pointers.set(e.pointerId, p);
    if (gesture.type === "pinch") {
      const [a, b] = [...pointers.values()];
      const k = clamp(gesture.k0 * Math.hypot(a[0] - b[0], a[1] - b[1]) / gesture.d0, 0.1, 3);
      cam.k = k;
      cam.x = (a[0] + b[0]) / 2 - gesture.anchor[0] * k;
      cam.y = (a[1] + b[1]) / 2 - gesture.anchor[1] * k;
      cameraTouched = true;
      applyCamera();
      return;
    }
    if (!gesture.moved && Math.hypot(p[0] - gesture.start[0], p[1] - gesture.start[1]) < 5) return;
    gesture.moved = true;
    if (gesture.type === "pan") {
      viewport.classList.add("panning");
      cam.x = gesture.cam0.x + p[0] - gesture.start[0];
      cam.y = gesture.cam0.y + p[1] - gesture.start[1];
      cameraTouched = true;
      applyCamera();
    } else {
      const n = gesture.node, [wx, wy] = toWorld(p);
      n.x = wx - gesture.grab[0];
      n.y = wy - gesture.grab[1];
      highlight(n.id);
      draw();
    }
  });
  const end = e => {
    if (!pointers.delete(e.pointerId) || !gesture) return;
    if (gesture.type === "node") {
      if (!gesture.moved && e.type === "pointerup") open(gesture.node.id);
      else highlight(null);
      if (gesture.moved) savePositions({ ...loadPositions(), [gesture.node.id]: [gesture.node.x, gesture.node.y] });
    }
    viewport.classList.remove("panning");
    gesture = null;
  };
  viewport.addEventListener("pointerup", end);
  viewport.addEventListener("pointercancel", end);
  viewport.addEventListener("wheel", e => {
    e.preventDefault();
    cameraTouched = true;
    // As in Figma: scrolling pans; a trackpad pinch (sent as ctrl+wheel) or ⌘/Ctrl+scroll zooms.
    if (e.ctrlKey || e.metaKey) {
      const [x, y] = local(e);
      const delta = -e.deltaY * (e.deltaMode === 1 ? 0.05 : e.deltaMode ? 1 : 0.002) * (e.ctrlKey ? 10 : 1);
      zoomAt(x, y, cam.k * Math.pow(2, delta));
    } else {
      const unit = e.deltaMode === 1 ? 16 : e.deltaMode ? viewport.clientHeight : 1;
      cam.x -= e.deltaX * unit;
      cam.y -= e.deltaY * unit;
      applyCamera();
    }
  }, { passive: false });
  viewport.addEventListener("keydown", e => {
    const pan = { ArrowLeft: [80, 0], ArrowRight: [-80, 0], ArrowUp: [0, 80], ArrowDown: [0, -80] }[e.key];
    if (pan) { cam.x += pan[0]; cam.y += pan[1]; cameraTouched = true; applyCamera(); }
    else if (e.key === "+" || e.key === "=") zoomBy(1.25);
    else if (e.key === "-") zoomBy(0.8);
    else if (e.key === "0") { cameraTouched = false; fit(); }
    else return;
    e.preventDefault();
  });

  $("zoom-in").onclick = () => zoomBy(1.25);
  $("zoom-out").onclick = () => zoomBy(0.8);
  $("zoom-fit").onclick = () => { cameraTouched = false; fit(); };
  $("relayout").onclick = relayout;

  const search = $("search");
  search.addEventListener("input", () => {
    const q = search.value.trim().toLowerCase();
    nodes.forEach(n => n.el.classList.toggle("muted", !!q && !n.id.toLowerCase().includes(q)));
  });
  search.addEventListener("keydown", e => {
    const q = search.value.trim().toLowerCase();
    if (e.key === "Enter" && q) { const hit = nodes.find(n => n.id.toLowerCase().includes(q)); if (hit) open(hit.id); }
    if (e.key === "Escape") { search.value = ""; search.dispatchEvent(new Event("input")); search.blur(); }
  });

  const legend = [];
  if (rootId) legend.push(el("span", {}, el("i", { class: "l-entry" }), "Entry point"));
  if (rootId && nodes.some(n => isUnreachable(n.id))) legend.push(el("span", {}, el("i", { class: "l-unreachable" }), "Unreachable"));
  if (screens.some(failures)) legend.push(el("span", {}, el("i", { class: "l-fail" }), "Failed renders"));
  if (links.some(l => l.back)) legend.push(el("span", {}, el("i", { class: "l-back" }), "Back to earlier screen"));
  if (links.length) legend.push(el("span", { class: "hide-sm" }, el("i", { class: "l-out" }), "Leads to"),
    el("span", { class: "hide-sm" }, el("i", { class: "l-in" }), "Reached from"));
  legend.push(el("span", { class: "hide-sm" }, "Drag to move · scroll to pan · pinch or ⌘ scroll to zoom"));
  $("legend").append(...legend);
  $("graph-notices").append(...notices());

  draw();
  applyCamera();
  return {
    show() {
      highlight(null);
      if (!cameraTouched) fit();
    },
  };
})();

// ---- Screen detail ---------------------------------------------------------------------------
let lightbox = { items: [], index: 0, opener: null };

function renderScreen(screen, deviceName) {
  const index = ordered.indexOf(screen);
  const prev = ordered[index - 1], next = ordered[index + 1];
  const failed = failures(screen);
  const from = incoming(screen.id), to = outgoing(screen.id);
  const chip = id => el("a", { class: "chip", href: screenUrl(id) }, thumbImg(byId.get(id), { loading: "lazy" }), id);
  const pagerLink = (target, label, key, back) => el("a", {
    class: "btn", href: target ? screenUrl(target.id) : null, "aria-disabled": !target && "true",
    "aria-label": target ? `${label}: ${target.id}` : label, "aria-keyshortcuts": key,
  }, back ? "←" : "", el("span", {}, target ? target.id : label), back ? "" : "→");

  const tabs = screen.devices.map(d => el("button", {
    type: "button", role: "tab", class: "tab", id: `tab-${d.name}`, "aria-controls": "panel", onclick: () => select(d),
  }, d.name,
    screen.cells.some(c => c.device === d.name && c.error) ? el("span", { class: "dot", role: "img", "aria-label": "has failed renders" }) : "",
    el("span", { class: "count" }, `${d.widthDp}×${d.heightDp}`)));
  const tablist = el("div", {
    class: "tabs", role: "tablist", "aria-label": "Device",
    onkeydown: e => {
      const i = tabs.indexOf(document.activeElement);
      const j = { ArrowRight: i + 1, ArrowLeft: i - 1, Home: 0, End: tabs.length - 1 }[e.key];
      if (i < 0 || j === undefined) return;
      e.preventDefault();
      const target = (j + tabs.length) % tabs.length;
      select(screen.devices[target]);
      tabs[target].focus();
    },
  }, tabs);

  let actual = store.get("zoom") === "actual";
  const panel = el("div", { role: "tabpanel", id: "panel", class: actual ? "actual" : null });
  const fitBtn = el("button", { type: "button", onclick: () => setZoom(false) }, "Fit");
  const actualBtn = el("button", { type: "button", title: "1 dp = 1 CSS pixel", onclick: () => setZoom(true) }, "Actual size");
  function setZoom(value) {
    actual = value;
    store.set("zoom", value ? "actual" : "fit");
    fitBtn.setAttribute("aria-pressed", String(!value));
    actualBtn.setAttribute("aria-pressed", String(value));
    panel.classList.toggle("actual", value);
  }
  setZoom(actual);

  function select(d) {
    tabs.forEach((tab, i) => {
      const selected = screen.devices[i] === d;
      tab.setAttribute("aria-selected", String(selected));
      tab.tabIndex = selected ? 0 : -1;
      if (selected) tab.scrollIntoView({ block: "nearest", inline: "nearest" });
    });
    panel.setAttribute("aria-labelledby", `tab-${d.name}`);
    history.replaceState(null, "", screenUrl(screen.id, d.name));
    const landscape = d.widthDp > d.heightDp;
    const items = [];
    panel.replaceChildren(...screen.samples.map(sample => el("section", { class: "state" },
      el("h2", {}, sample),
      el("div", { class: "shots" + (landscape ? " landscape" : "") }, screen.themes.map(theme => {
        const cell = screen.cells.find(c => c.device === d.name && c.theme === theme && c.sample === sample);
        const caption = el("figcaption", {}, el("i", { style: `background:${theme === "Dark" ? "#16181c" : "#fff"}` }), theme);
        if (!cell || cell.error) {
          return el("figure", { class: "shot" }, el("div", { class: "shot-error" },
            el("strong", {}, "Render failed"), el("code", {}, cell ? cell.error : "Not generated")), caption);
        }
        const position = items.push({ cell, screen, device: d }) - 1;
        return el("figure", { class: "shot" }, el("button", {
          type: "button", class: "frame", "aria-label": `Open ${sample}, ${theme} full screen`, style: frameSize(d),
          onclick: e => openLightbox(items, position, e.currentTarget),
        }, framed(d, el("img", {
          src: imageUrl(cell), alt: `${screen.id}, ${sample}, ${theme}, ${d.name}`, loading: "lazy", decoding: "async",
          width: d.widthDp * DENSITY, height: d.heightDp * DENSITY,
          onload: e => e.target.closest(".frame").classList.add("loaded"),
          onerror: e => e.target.closest(".frame").replaceWith(el("div", { class: "shot-error" },
            el("strong", {}, "Image missing"), el("code", {}, cell.image))),
        }))), caption);
      })),
    )));
  }

  page.replaceChildren(
    el("div", { class: "detail-head" },
      el("div", {},
        el("div", { class: "title" }, el("h1", {}, screen.id),
          screen.id === entry?.id ? el("span", { class: "badge entry" }, "Entry point") : "",
          screen.group ? el("span", { class: "badge" }, screen.group) : "",
          entry && isUnreachable(screen.id) ? el("span", { class: "badge" }, "Unreachable") : "",
          failed ? el("span", { class: "badge bad" }, `${failed} failed`) : ""),
        el("p", { class: "meta" }, [plural(screen.samples.length, "state"), screen.themes.join(" & "),
          plural(screen.devices.length, "device"), entry && depth.has(screen.id) ? plural(depth.get(screen.id), "hop") + " from start" : null]
          .filter(Boolean).join(" · "))),
      el("nav", { class: "pager", "aria-label": "Screens" },
        pagerLink(prev, "Previous", "[", true), pagerLink(next, "Next", "]", false))),
    el("div", { class: "flow" },
      el("section", {}, el("h2", {}, "Reached from"),
        el("div", { class: "chips" }, from.length ? from.map(chip)
          : el("span", { class: "none" }, screen.id === entry?.id ? "App start" : "No screen navigates here"))),
      el("section", {}, el("h2", {}, "Navigates to"),
        el("div", { class: "chips" }, to.length ? to.map(chip) : el("span", { class: "none" }, "Nothing yet — add navigatesTo"))),
      siblings(screen.id).length ? el("section", {}, el("h2", {}, screen.group), el("div", { class: "chips" }, siblings(screen.id).map(chip))) : ""),
    el("div", { class: "toolbar" }, tablist, el("div", { class: "seg", role: "group", "aria-label": "Image size" }, fitBtn, actualBtn)),
    panel,
  );
  select(deviceOf(screen, deviceName) || screen.devices[0]);
  document.title = `${screen.id} · Auto Preview`;
  setCrumbs([["App graph", "#/"], [screen.id]]);
}

function openLightbox(items, index, opener) {
  lightbox = { items, index, opener };
  showLightboxItem();
  $("lightbox").showModal();
}
function showLightboxItem() {
  const { cell, screen, device } = lightbox.items[lightbox.index];
  const img = $("lb-img");
  img.src = imageUrl(cell);
  img.alt = `${screen.id}, ${cell.sample}, ${cell.theme}, ${device.name}`;
  img.width = device.widthDp * DENSITY;
  img.height = device.heightDp * DENSITY;
  img.className = device.round ? "round" : "";
  $("lb-title").textContent = `${screen.id} › ${cell.sample}`;
  $("lb-sub").textContent = `${cell.theme} · ${device.name} · ${device.widthDp}×${device.heightDp} dp`;
  $("lb-count").textContent = `${lightbox.index + 1} / ${lightbox.items.length}`;
  $("lb-open").href = imageUrl(cell);
}
function stepLightbox(delta) {
  lightbox.index = (lightbox.index + delta + lightbox.items.length) % lightbox.items.length;
  showLightboxItem();
}
{
  const dialog = $("lightbox");
  $("lb-prev").onclick = () => stepLightbox(-1);
  $("lb-next").onclick = () => stepLightbox(1);
  $("lb-close").onclick = () => dialog.close();
  dialog.addEventListener("click", e => { if (e.target === dialog || e.target === $("lb-stage")) dialog.close(); });
  dialog.addEventListener("close", () => lightbox.opener?.focus());
  dialog.addEventListener("keydown", e => {
    if (e.key === "ArrowRight") stepLightbox(1);
    else if (e.key === "ArrowLeft") stepLightbox(-1);
    else if (e.key === "t" || e.key === "T") {
      const { cell } = lightbox.items[lightbox.index];
      const other = lightbox.items.findIndex(i => i.cell.sample === cell.sample && i.cell.theme !== cell.theme);
      if (other >= 0) { lightbox.index = other; showLightboxItem(); }
    } else return;
    e.preventDefault();
  });
  let touchX = null;
  dialog.addEventListener("touchstart", e => touchX = e.touches[0].clientX, { passive: true });
  dialog.addEventListener("touchend", e => {
    const dx = e.changedTouches[0].clientX - touchX;
    if (touchX != null && Math.abs(dx) > 50) stepLightbox(dx < 0 ? 1 : -1);
    touchX = null;
  });
}

// ---- List ----------------------------------------------------------------------------------
function renderList() {
  const links = ids => ids.length
    ? el("div", { class: "chips" }, ids.map(id => el("a", { class: "chip", href: screenUrl(id) }, id)))
    : el("span", { class: "none" }, "—");
  page.replaceChildren(
    el("div", { class: "list-head" }, el("h1", {}, "Screens"), ...notices()),
    el("div", { class: "table-wrap" }, el("table", {},
      el("thead", {}, el("tr", {}, ["Screen", "Group", "Hops", "Reached from", "Navigates to", "States", "Devices", "Status"]
        .map(h => el("th", { scope: "col" }, h)))),
      el("tbody", {}, ordered.map(s => {
        const failed = failures(s);
        return el("tr", {},
          el("th", { scope: "row" }, el("a", { class: "screen-cell", href: screenUrl(s.id) }, thumbImg(s, { loading: "lazy" }), s.id)),
          el("td", {}, s.group ?? el("span", { class: "none" }, "—")),
          el("td", { class: "num" }, s.id === entry?.id ? el("span", { class: "badge entry" }, "Entry")
            : entry && isUnreachable(s.id) ? el("span", { class: "badge" }, "Unreachable") : depth.has(s.id) ? depth.get(s.id) : "—"),
          el("td", {}, links(incoming(s.id))),
          el("td", {}, links(outgoing(s.id))),
          el("td", { class: "num" }, s.samples.length),
          el("td", {}, s.devices.map(d => d.name).join(", ")),
          el("td", {}, failed ? el("span", { class: "badge bad" }, `${failed} failed`) : el("span", { class: "badge" }, "OK")),
        );
      })),
    )),
  );
  document.title = "Screens · Auto Preview";
  setCrumbs([["Screens"]]);
}

// ---- Routing -------------------------------------------------------------------------------
function setCrumbs(items) {
  $("crumbs").replaceChildren(el("ol", {}, items.map(([label, href], i) => el("li", {},
    href ? el("a", { href }, label) : el("span", { "aria-current": i === items.length - 1 ? "page" : null }, label)))));
}
function route() {
  const [kind, id, device] = location.hash.replace(/^#\/?/, "").split("/").map(decodeURIComponent);
  const view = kind === "screen" && byId.has(id) ? "screen" : kind === "list" ? "list" : "graph";
  document.body.classList.toggle("graph-mode", view === "graph");
  $("graph-view").hidden = view !== "graph";
  page.hidden = view === "graph";
  for (const [link, active] of [[$("nav-graph"), view === "graph"], [$("nav-list"), view === "list"]]) {
    if (active) link.setAttribute("aria-current", "page"); else link.removeAttribute("aria-current");
  }
  if (view === "screen") renderScreen(byId.get(id), device);
  else if (view === "list") renderList();
  else {
    page.replaceChildren();
    document.title = "Auto Preview";
    setCrumbs([]);
    graph.show();
  }
  window.scrollTo(0, 0);
}
addEventListener("hashchange", route);
addEventListener("keydown", e => {
  if (e.target.closest?.("input, dialog") || e.metaKey || e.ctrlKey || e.altKey) return;
  if (e.key === "/" && !$("graph-view").hidden) { e.preventDefault(); $("search").focus(); }
  if ((e.key === "[" || e.key === "]") && !page.hidden) {
    const link = page.querySelectorAll(".pager a")[e.key === "[" ? 0 : 1];
    if (link?.getAttribute("href")) location.hash = link.getAttribute("href");
  }
});
route();
