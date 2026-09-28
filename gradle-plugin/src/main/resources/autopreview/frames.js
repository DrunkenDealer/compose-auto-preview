// Phone, tablet, TV and watch wear Android Studio's device art (AOSP device-art-resources, Apache 2.0): the bezel with
// the screen's corners and punch-hole camera merged into one overlay. Sizes are in the art's pixels, and each screen
// matches its device's dp size.
const DEVICE_ART = {
  Phone: { src: "pixel_7_pro", width: 1547, height: 3272, screen: [48, 66, 1440, 3120] },
  Tablet: { src: "pixel_tablet", width: 2798, height: 1837, screen: [119, 117, 2560, 1600] },
  Tv: { src: "tv_1080p", width: 2090, height: 1255, screen: [85, 59, 1920, 1080] },
  Wear: { src: "wearos_small_round", width: 456, height: 456, screen: [36, 36, 384, 384], radius: 192 },
};

// Foldable and desktop have no art of their size, so their bezels are drawn from geometry, ported from Vitrine's
// frames.js. Geometry is authored with the screen's short side at 1080 units so the hairlines, rails and cameras keep
// Vitrine's proportions at any device size. Graphite rather than Vitrine's black: black dissolves into the report's dark
// theme, graphite reads on both.
const FRAME_TINT = { edge: "#6b6f78", core: "#3c3f46", rim: "#a3a8b2" };

const fr = v => Math.round(v * 100) / 100;
const rrect = (x, y, w, h, r, attrs) =>
  `<rect x="${fr(x)}" y="${fr(y)}" width="${fr(w)}" height="${fr(h)}" rx="${fr(r)}" ry="${fr(r)}" ${attrs}/>`;
const dot = (cx, cy, r, attrs) => `<circle cx="${fr(cx)}" cy="${fr(cy)}" r="${fr(r)}" ${attrs}/>`;

function geom(meta) {
  const { frameWidth: W, frameHeight: H, outerRadius: R, cutout: c } = meta;
  return { W, H, R, c, landscape: W > H, depth: Math.max(3, Math.round(Math.min(c.width, c.height) * 0.0075)) };
}
const inset = (g, d) => ({ x: d, y: d, w: g.W - d * 2, h: g.H - d * 2, r: Math.max(0, g.R - d) });

// Buttons and cameras are authored in portrait terms; a landscape frame is the same slab given a quarter-turn.
const TURNED = {
  ccw: { left: "bottom", right: "top", top: "left", bottom: "right" },
  cw: { left: "top", right: "bottom", top: "right", bottom: "left" },
};
const edgeFor = (g, turn, side) => (g.landscape ? TURNED[turn][side] : side);

function nub(g, edge, a, b, fill) {
  const d = g.depth, r = d * 0.8;
  if (edge === "left") return rrect(0, a, d * 2, b - a, r, `fill="${fill}"`);
  if (edge === "right") return rrect(g.W - d * 2, a, d * 2, b - a, r, `fill="${fill}"`);
  if (edge === "top") return rrect(a, 0, b - a, d * 2, r, `fill="${fill}"`);
  return rrect(a, g.H - d * 2, b - a, d * 2, r, `fill="${fill}"`);
}
const railButtons = (g, turn, specs, fill) => specs.map(({ side, from, to }) => {
  const alongLong = side === "left" || side === "right";
  const [long, short] = g.landscape ? [g.W, g.H] : [g.H, g.W];
  const len = alongLong ? long : short;
  let [a, b] = [len * from, len * to];
  if (g.landscape && (turn === "cw") === alongLong) [a, b] = [len - b, len - a];
  return nub(g, edgeFor(g, turn, side), a, b, fill);
}).join("");


const PIXEL_RAILS = [{ side: "right", from: 0.19, to: 0.258 }, { side: "right", from: 0.282, to: 0.4 }];


// A book-style foldable, open: the hinge spine standing proud at the top and bottom edges, a gap splitting the bezel
// into two halves, a faint crease down the screen and the inner camera in the right half's bezel.
function fold(g, t) {
  const { c, depth: d } = g, x = g.W / 2, spineW = c.width * 0.05, gap = Math.max(3, c.width * 0.006);
  const band = c.y - d, crease = c.width * 0.03;
  const split = (y, h) => rrect(x - gap / 2, y, gap, h, 0, 'fill="#000" fill-opacity="0.55"');
  return {
    body: inset(g, d),
    under: rrect(x - spineW / 2, 0, spineW, g.H, d, `fill="${t.edge}" stroke="${t.rim}" stroke-width="2"`) +
      railButtons(g, "ccw", PIXEL_RAILS, t.edge),
    over: split(d, band) + split(c.y + c.height, g.H - d - c.y - c.height) +
      dot(c.x + c.width * 0.78, d + band / 2, band * 0.3, `fill="#08080a" stroke="${t.rim}" stroke-width="${fr(band * 0.08)}"`) +
      `<defs><linearGradient id="crease"><stop offset="0" stop-color="#000" stop-opacity="0"/>` +
      `<stop offset=".45" stop-color="#000" stop-opacity=".1"/><stop offset=".55" stop-color="#fff" stop-opacity=".12"/>` +
      `<stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient></defs>` +
      rrect(x - crease / 2, c.y, crease, c.height, 0, 'fill="url(#crease)"'),
  };
}

// A panel on a pedestal: whatever height the geometry declares below the panel is the neck and foot.
function stand(g, t) {
  const panelH = g.c.y * 2 + g.c.height, neckW = g.W * 0.085, neckH = (g.H - panelH) * 0.62;
  const footH = g.H - panelH - neckH, footW = g.W * 0.34;
  return {
    body: { x: 0, y: 0, w: g.W, h: panelH, r: g.R },
    under: rrect((g.W - neckW) / 2, panelH - g.R, neckW, neckH + g.R, g.R * 0.4, `fill="${t.core}"`) +
      rrect((g.W - footW) / 2, g.H - footH, footW, footH, footH * 0.45, 'fill="url(#bezel)"'),
    over: "",
  };
}


const FRAME_STYLES = { fold, stand };

// Per device: style, and bezel, corner radii and stand height as fractions of the screen's short side.
const FRAME_SPECS = {
  Foldable: { style: "fold", margin: 0.05, outer: 0.07, screen: 0.035 },
  Desktop: { style: "stand", margin: 0.03, outer: 0.03, screen: 0.012, stand: 0.2 },
};

function frameMeta(device, bezel) {
  const spec = FRAME_SPECS[device.name] || FRAME_SPECS.Desktop;
  const k = 1080 / Math.min(device.widthDp, device.heightDp);
  const w = device.widthDp * k, h = device.heightDp * k;
  const m = spec.margin * bezel * 1080;
  return { style: spec.style, frameWidth: w + m * 2, frameHeight: h + m * 2 + (spec.stand || 0) * 1080,
    outerRadius: (spec.screen + (spec.outer - spec.screen) * bezel) * 1080,
    cutout: { x: m, y: m, width: w, height: h, borderRadius: spec.screen * 1080 } };
}

function frameSvg(meta, t = FRAME_TINT) {
  const g = geom(meta), { W, H, c } = g;
  const { body: b, under, over } = FRAME_STYLES[meta.style](g, t);
  const sw = Math.max(2, Math.min(W, H) * 0.0022), gw = Math.max(1.5, Math.min(W, H) * 0.004);
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${fr(W)}" height="${fr(H)}" viewBox="0 0 ${fr(W)} ${fr(H)}">
  <defs>
    <mask id="cutout"><rect width="${fr(W)}" height="${fr(H)}" fill="white"/>${rrect(c.x, c.y, c.width, c.height, c.borderRadius, 'fill="black"')}</mask>
    <linearGradient id="bezel" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="${t.edge}"/><stop offset="0.5" stop-color="${t.core}"/><stop offset="1" stop-color="${t.edge}"/>
    </linearGradient>
  </defs>
  <g mask="url(#cutout)">
    ${under}
    ${rrect(b.x, b.y, b.w, b.h, b.r, 'fill="url(#bezel)"')}
    ${rrect(c.x - gw, c.y - gw, c.width + gw * 2, c.height + gw * 2, c.borderRadius + gw, `fill="none" stroke="#000" stroke-opacity="0.55" stroke-width="${fr(gw * 2)}"`)}
    ${rrect(b.x + sw / 2, b.y + sw / 2, b.w - sw, b.h - sw, Math.max(0, b.r - sw / 2), `fill="none" stroke="${t.rim}" stroke-width="${fr(sw)}"`)}
  </g>
  ${over}
</svg>`;
}

// Frame box in dp, and where the screen sits in it (fractions), cached per device. `bezel` thickens a drawn bezel for
// thumbnails, where a true-to-life one is a hairline.
const frames = new Map();
function artFrame(art) {
  const [x, y, width, height] = art.screen;
  return { W: art.width, H: art.height, c: { x, y, width, height, borderRadius: art.radius ?? 0 }, url: `assets/frames/${art.src}.webp` };
}
function drawnFrame(device, bezel) {
  const meta = frameMeta(device, bezel);
  return { W: meta.frameWidth, H: meta.frameHeight, c: meta.cutout, url: `data:image/svg+xml,${encodeURIComponent(frameSvg(meta))}` };
}
function deviceFrame(device, bezel = 1) {
  const art = DEVICE_ART[device.name] || (device.round ? DEVICE_ART.Wear : null);
  const key = art ? device.name : device.name + bezel;
  if (!frames.has(key)) {
    const { W, H, c, url } = art ? artFrame(art) : drawnFrame(device, bezel), k = device.widthDp / c.width;
    frames.set(key, {
      width: W * k, height: H * k, url,
      style: `left:${c.x / W * 100}%;top:${c.y / H * 100}%;width:${c.width / W * 100}%;height:${c.height / H * 100}%;` +
        `border-radius:${c.borderRadius / c.width * 100}% / ${c.borderRadius / c.height * 100}%`,
    });
  }
  return frames.get(key);
}

// The screenshot inside its bezel. Sized by the caller through width; the box keeps the frame's aspect ratio.
function framed(device, img, bezel) {
  const f = deviceFrame(device, bezel);
  img.classList.add("screen");
  img.style.cssText += f.style;
  return el("span", { class: "device", style: `aspect-ratio:${f.width} / ${f.height}` },
    img, el("img", { class: "bezel", src: f.url, alt: "", draggable: "false" }));
}
