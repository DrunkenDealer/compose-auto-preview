const DENSITY = DATA.density || 2;
const screens = DATA.screens;
const byId = new Map(screens.map(s => [s.id, s]));
const $ = id => document.getElementById(id);
const page = $("page");
const reduceMotion = matchMedia("(prefers-reduced-motion: reduce)").matches;
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
const edges = screens.flatMap(s => [...new Set(s.navigatesTo)].filter(t => byId.has(t) && t !== s.id).map(t => ({ source: s.id, target: t })));
const outgoing = id => edges.filter(e => e.source === id).map(e => e.target);
const incoming = id => edges.filter(e => e.target === id).map(e => e.source);
const unknownTargets = screens.flatMap(s => s.navigatesTo.filter(t => !byId.has(t)).map(t => `${s.id} → ${t}`));
const entry = screens.find(s => s.entryPoint);

// Hops from the entry point (BFS over navigatesTo). Without one, screens nobody navigates to act as roots.
const depth = new Map();
{
  let roots = entry ? [entry.id] : screens.filter(s => !incoming(s.id).length).map(s => s.id);
  if (!roots.length && screens.length) roots = [screens[0].id];
  const queue = [...roots];
  roots.forEach(id => depth.set(id, 0));
  while (queue.length) {
    const id = queue.shift();
    for (const t of outgoing(id)) if (!depth.has(t)) { depth.set(t, depth.get(id) + 1); queue.push(t); }
  }
}
const maxDepth = Math.max(0, ...depth.values());
const hops = id => depth.has(id) ? depth.get(id) : maxDepth + 1;
const isUnreachable = id => !depth.has(id);
const ordered = [...screens].sort((a, b) => hops(a.id) - hops(b.id) || a.id.localeCompare(b.id));

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
// Force layout after d3-force: velocity Verlet with many-body repulsion, link springs, collision and a
// radial force placing each screen on the ring of its hop count, with the entry point pinned at the centre.
const graph = (() => {
  const viewport = $("viewport"), world = $("world"), svg = $("edges");
  const ALPHA_MIN = 0.001, ALPHA_DECAY = 1 - Math.pow(0.001, 1 / 300), VELOCITY_DECAY = 0.4;
  const CHARGE = -1400, CHARGE_MAX = 900, LINK_DISTANCE = 230, RING = 300, RADIAL = 0.3, LABEL_HEIGHT = 32, TAG_HEIGHT = 34;
  const sim = { alpha: 1, alphaTarget: 0 };
  const cam = { x: 0, y: 0, k: 1 };
  let cameraTouched = false, frame = 0, gesture = null;

  const rootId = entry ? entry.id : null;
  const nodes = ordered.map(screen => {
    const t = thumbnail(screen), f = t && deviceFrame(t.device, THUMB_BEZEL);
    const ratio = f ? f.width / f.height : 0.5;
    let h = screen.id === rootId ? 180 : 116 + 14 * Math.min(incoming(screen.id).length, 3);
    let w = h * ratio;
    if (w > 240) { w = 240; h = w / ratio; }
    const full = h + LABEL_HEIGHT + (screen.id === rootId ? TAG_HEIGHT : 0);
    return { id: screen.id, screen, tw: w, th: h, w: Math.max(w, 80), h: full, vx: 0, vy: 0, x: 0, y: 0,
      r: Math.max(w, full) / 2 + 18 };
  });
  const nodeById = new Map(nodes.map(n => [n.id, n]));
  const links = edges.map(e => ({ ...e, s: nodeById.get(e.source), t: nodeById.get(e.target),
    curved: edges.some(o => o.source === e.target && o.target === e.source) }));
  const degree = id => links.filter(l => l.source === id || l.target === id).length;
  links.forEach(l => {
    const cs = degree(l.source), ct = degree(l.target);
    l.strength = 1 / Math.min(cs, ct);
    l.bias = cs / (cs + ct);
  });
  const neighbours = id => new Set([id, ...outgoing(id), ...incoming(id)]);

  // Deterministic start: spread each ring evenly so the simulation only needs to untangle.
  const rings = new Map();
  for (const n of nodes) {
    const d = hops(n.id);
    if (!rings.has(d)) rings.set(d, []);
    rings.get(d).push(n);
  }
  rings.forEach((ring, d) => ring.forEach((n, i) => {
    const angle = (i / ring.length) * 2 * Math.PI + d * 0.6;
    n.x = Math.cos(angle) * d * RING;
    n.y = Math.sin(angle) * d * RING;
  }));
  if (rootId) { const root = nodeById.get(rootId); root.fx = root.fy = 0; }

  function tick() {
    sim.alpha += (sim.alphaTarget - sim.alpha) * ALPHA_DECAY;
    const alpha = sim.alpha;
    for (let i = 0; i < nodes.length; i++) {
      const a = nodes[i];
      for (let j = i + 1; j < nodes.length; j++) {
        const b = nodes[j];
        let x = b.x - a.x, y = b.y - a.y, l = x * x + y * y;
        if (l === 0) { x = (Math.random() - .5) * 1e-6; y = (Math.random() - .5) * 1e-6; l = x * x + y * y; }
        if (l > CHARGE_MAX * CHARGE_MAX) continue;
        const f = CHARGE * alpha / l;
        a.vx += x * f; a.vy += y * f;
        b.vx -= x * f; b.vy -= y * f;
      }
    }
    for (const link of links) {
      const { s, t } = link;
      const x = t.x + t.vx - s.x - s.vx || 1e-6, y = t.y + t.vy - s.y - s.vy || 1e-6;
      const l = Math.hypot(x, y), k = (l - LINK_DISTANCE) / l * alpha * link.strength;
      t.vx -= x * k * link.bias; t.vy -= y * k * link.bias;
      s.vx += x * k * (1 - link.bias); s.vy += y * k * (1 - link.bias);
    }
    for (const n of nodes) {
      const target = hops(n.id) * RING, l = Math.hypot(n.x, n.y) || 1e-6, k = (target - l) * RADIAL * alpha / l;
      n.vx += n.x * k; n.vy += n.y * k;
    }
    for (let i = 0; i < nodes.length; i++) {
      const a = nodes[i];
      for (let j = i + 1; j < nodes.length; j++) {
        const b = nodes[j];
        const x = b.x + b.vx - a.x - a.vx, y = b.y + b.vy - a.y - a.vy, l = Math.hypot(x, y) || 1e-6, min = a.r + b.r;
        if (l >= min) continue;
        const k = (min - l) / l * 0.35;
        a.vx -= x * k; a.vy -= y * k;
        b.vx += x * k; b.vy += y * k;
      }
    }
    for (const n of nodes) {
      if (n.fx != null) { n.x = n.fx; n.vx = 0; } else { n.x += n.vx *= 1 - VELOCITY_DECAY; }
      if (n.fy != null) { n.y = n.fy; n.vy = 0; } else { n.y += n.vy *= 1 - VELOCITY_DECAY; }
    }
  }

  // Edges attach to the thumbnail's width and the full height down to the label, so arrowheads stay visible.
  function boundary(n, ux, uy, pad) {
    const hw = n.tw / 2 + pad, hh = n.h / 2 + pad;
    const s = Math.min(ux ? hw / Math.abs(ux) : Infinity, uy ? hh / Math.abs(uy) : Infinity);
    return [n.x + ux * s, n.y + uy * s];
  }
  function edgePath({ s, t, curved }) {
    // Nodes dropped exactly on each other have no direction; point the edge down instead of producing NaN.
    const [dx, dy] = t.x !== s.x || t.y !== s.y ? [t.x - s.x, t.y - s.y] : [0, 1], len = Math.hypot(dx, dy);
    if (!curved) {
      const [x1, y1] = boundary(s, dx / len, dy / len, 4), [x2, y2] = boundary(t, -dx / len, -dy / len, 6);
      return `M${x1} ${y1}L${x2} ${y2}`;
    }
    // Two-way navigation: bend each direction to its own side.
    const cx = (s.x + t.x) / 2 - dy / len * 60, cy = (s.y + t.y) / 2 + dx / len * 60;
    const towards = n => { const x = cx - n.x, y = cy - n.y, l = Math.hypot(x, y) || 1; return [x / l, y / l]; };
    const [x1, y1] = boundary(s, ...towards(s), 4), [x2, y2] = boundary(t, ...towards(t), 6);
    return `M${x1} ${y1}Q${cx} ${cy} ${x2} ${y2}`;
  }

  for (const link of links) {
    link.path = document.createElementNS("http://www.w3.org/2000/svg", "path");
    link.path.setAttribute("class", "edge");
    svg.append(link.path);
  }
  for (const n of nodes) {
    const failed = failures(n.screen);
    const unreachable = rootId && isUnreachable(n.id);
    const label = [n.id, n.id === rootId && "entry point", unreachable && "unreachable from entry point",
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
    for (const l of links) l.path.setAttribute("d", edgePath(l));
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
    const minX = Math.min(...nodes.map(n => n.x - n.w / 2)), maxX = Math.max(...nodes.map(n => n.x + n.w / 2));
    const minY = Math.min(...nodes.map(n => n.y - n.h / 2)), maxY = Math.max(...nodes.map(n => n.y + n.h / 2));
    // Keep clear of the search bar on top and the zoom controls / legend at the bottom-right.
    const top = 72, right = 72, bottom = 64, pad = 24;
    cam.k = clamp(Math.min((W - pad - right) / (maxX - minX), (H - top - bottom) / (maxY - minY)), 0.1, 1.2);
    cam.x = pad + (W - pad - right) / 2 - (minX + maxX) / 2 * cam.k;
    cam.y = top + (H - top - bottom) / 2 - (minY + maxY) / 2 * cam.k;
    applyCamera();
  }

  function step() {
    frame = 0;
    tick();
    draw();
    if (!cameraTouched) fit();
    if (sim.alpha >= ALPHA_MIN || sim.alphaTarget > 0) run();
  }
  function run() {
    if (reduceMotion) {
      while (sim.alpha >= ALPHA_MIN) tick();
      draw();
      if (!cameraTouched) fit();
    } else if (!frame && !$("graph-view").hidden) {
      frame = requestAnimationFrame(step);
    }
  }
  function reheat(alpha) { sim.alpha = Math.max(sim.alpha, alpha); run(); }

  function highlight(id) {
    viewport.classList.toggle("focus", !!id);
    const near = id ? neighbours(id) : new Set();
    nodes.forEach(n => n.el.classList.toggle("hot", near.has(n.id)));
    links.forEach(l => l.path.classList.toggle("hot", !!id && (l.source === id || l.target === id)));
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
      n.fx = wx - gesture.grab[0];
      n.fy = wy - gesture.grab[1];
      highlight(n.id);
      if (reduceMotion) { n.x = n.fx; n.y = n.fy; draw(); } else { sim.alphaTarget = 0.3; reheat(0.3); }
    }
  });
  const end = e => {
    if (!pointers.delete(e.pointerId) || !gesture) return;
    if (gesture.type === "node") {
      const n = gesture.node;
      if (!gesture.moved && e.type === "pointerup") open(n.id);
      else {
        sim.alphaTarget = 0;
        if (n.id !== rootId) n.fx = n.fy = null;
        highlight(null);
      }
    }
    viewport.classList.remove("panning");
    gesture = null;
  };
  viewport.addEventListener("pointerup", end);
  viewport.addEventListener("pointercancel", end);
  viewport.addEventListener("wheel", e => {
    e.preventDefault();
    const [x, y] = local(e);
    const delta = -e.deltaY * (e.deltaMode === 1 ? 0.05 : e.deltaMode ? 1 : 0.002) * (e.ctrlKey ? 10 : 1);
    cameraTouched = true;
    zoomAt(x, y, cam.k * Math.pow(2, delta));
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
  $("relayout").onclick = () => { cameraTouched = false; reheat(1); };

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
  legend.push(el("span", { class: "hide-sm" }, "Drag to move · scroll to zoom"));
  $("legend").append(...legend);
  $("graph-notices").append(...notices());

  draw();
  applyCamera();
  return {
    show() {
      highlight(null);
      if (!cameraTouched) fit();
      run();
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
        el("div", { class: "chips" }, to.length ? to.map(chip) : el("span", { class: "none" }, "Nothing yet — add navigatesTo")))),
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
      el("thead", {}, el("tr", {}, ["Screen", "Hops", "Reached from", "Navigates to", "States", "Devices", "Status"]
        .map(h => el("th", { scope: "col" }, h)))),
      el("tbody", {}, ordered.map(s => {
        const failed = failures(s);
        return el("tr", {},
          el("th", { scope: "row" }, el("a", { class: "screen-cell", href: screenUrl(s.id) }, thumbImg(s, { loading: "lazy" }), s.id)),
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
