// =====================================================================
// GrowlView - Growl's display: its three curves and its modulation matrix
//
// Left, one transfer curve per stage, in the stage's colour: input along,
// output up, the same arithmetic the server runs, the input's level behind
// it. The stage the panel shows (`focus`) is framed; a stage the routing has
// silenced is dimmed and says so.
//
//   click a curve              show that stage's controls (focusAction)
//   drag a curve up and down   its drive (shift: fine)
//   the wheel on a curve       its bias
//
// Right, the matrix: a row per source, with a light showing what it does
// now, a column per destination (a stage's in the stage's colour); a cell
// is a bar from the middle, up for more, down for less.
//
//   drag a cell up and down    how much that source moves that destination
//   double click a cell        none
// =====================================================================

GrowlView : FxParamView {
	var dragKey, dragY, dragFrom, dragShift = false;
	var <focus = 0, <>focusAction;

	*minHeight { ^200 }

	*destLabels { ^#["drv 1", "drv 2", "drv 3", "frq 1", "frq 2", "frq 3", "fb", "tone"] }

	// the stage each destination belongs to, or nil
	*destStage { |col| ^#[0, 1, 2, 0, 1, 2, nil, nil][col] }

	focus_ { |i| focus = i; this.refresh }

	prSplit { ^(bw * 0.56).round }

	prCurveRect { |i|
		var w = (this.prSplit - 16 - 16) / 3;
		^Rect(8 + (i * (w + 8)), 24, w, bh - 32)
	}

	prMatrixRect {
		var x0 = this.prSplit + 64;
		^Rect(x0, 42, bw - x0 - 8, bh - 50)
	}

	prCell { |x, y|
		var m = this.prMatrixRect, sources = Growl.sources, dests = Growl.dests, col, row;
		if (m.containsPoint(x @ y).not) { ^nil };
		col = ((x - m.left) / m.width * dests.size).floor.asInteger.clip(0, dests.size - 1);
		row = ((y - m.top) / m.height * sources.size).floor.asInteger.clip(0, sources.size - 1);
		^Growl.modKey(sources[row], dests[col])
	}

	prStageActive { |i| ^Growl.stageActive(i, unit.routing) }

	prDraw {
		this.prCaption("stages · " ++ unit.routing, 8, 4, FxPalette.text);
		3.do { |i| this.prDrawCurve(i) };
		Pen.fillColor = FxPalette.card;
		Pen.fillRect(Rect(this.prSplit - 4, 8, 1, bh - 16));
		this.prDrawMatrix;
	}

	prDrawCurve { |i|
		var r = this.prCurveRect(i), n = i + 1;
		var type = unit.get(("type" ++ n).asSymbol), drive = unit.get(("drive" ++ n).asSymbol);
		var bias = unit.get(("bias" ++ n).asSymbol);
		var on = this.prStageActive(i), col = Growl.stageColour(i);
		var level = unit.inLevel.clip(0, 1);
		var yOf = { |v| r.top + (r.height * 0.5) - (v.clip(-1.2, 1.2) * r.height * 0.4) };
		var xOf = { |v| r.left + ((v + 1) * 0.5 * r.width) };
		Pen.fillColor = FxPalette.bg;
		Pen.addRoundedRect(r, 3, 3);
		Pen.fill;
		// the grid: -1, -0.5, 0, 0.5, 1 both ways
		Pen.width = 1;
		[-1, -0.5, 0, 0.5, 1].do { |v|
			Pen.strokeColor = FxPalette.fade(FxPalette.waveDim, if (v == 0) { 0.9 } { 0.35 });
			Pen.line(r.left @ yOf.(v), r.right @ yOf.(v));
			Pen.line(xOf.(v) @ r.top, xOf.(v) @ r.bottom);
			Pen.stroke;
		};
		// how much of the curve the input is using
		if (on and: { level > 0.001 }) {
			Pen.fillColor = FxPalette.fade(FxPalette.wave, 0.12);
			Pen.fillRect(Rect(xOf.(level.neg), r.top, xOf.(level) - xOf.(level.neg), r.height));
		};
		Pen.strokeColor = if (on) { col } { FxPalette.waveDim };
		Pen.width = if (i == focus) { 2 } { 1.5 };
		(r.width.asInteger.max(2)).do { |px|
			var x = px / (r.width - 1) * 2 - 1;
			var p = (r.left + px) @ yOf.(Growl.shape(x, type, drive, bias));
			if (px == 0) { Pen.moveTo(p) } { Pen.lineTo(p) };
		};
		Pen.stroke;
		if (i == focus) {
			Pen.strokeColor = FxPalette.fade(col, 0.8);
			Pen.width = 1.5;
			Pen.addRoundedRect(r.insetBy(0.75, 0.75), 3, 3);
			Pen.stroke;
		};
		Pen.stringLeftJustIn("% %".format(n, type), Rect(r.left + 6, r.top + 3, r.width - 12, 14),
			FxPalette.font, if (on) { col } { FxPalette.dim });
		Pen.stringRightJustIn(if (on) { FxPalette.format(drive, "dB", nil, 1) } { "off" },
			Rect(r.left + 6, r.top + 3, r.width - 12, 14), FxPalette.smallFont, FxPalette.dim);
		Pen.stringRightJustIn("in", Rect(r.left, r.bottom - 14, r.width - 4, 12), FxPalette.smallFont,
			FxPalette.fade(FxPalette.dim, 0.7));
	}

	prDrawMatrix {
		var m = this.prMatrixRect, sources = Growl.sources, dests = Growl.dests;
		var cw = m.width / dests.size, ch = m.height / sources.size;
		var live = unit.extras ? [];
		this.prCaption("modulation", this.prSplit + 8, 4, FxPalette.text);
		dests.do { |dest, col|
			var stage = this.class.destStage(col);
			Pen.stringCenteredIn(this.class.destLabels[col], Rect(m.left + (col * cw), m.top - 16, cw, 14),
				FxPalette.smallFont, stage !? { |s| Growl.stageColour(s) } ? FxPalette.dim);
		};
		sources.do { |src, row|
			var y = m.top + (row * ch);
			var light = live[row] ? 0;
			Pen.stringLeftJustIn(src.asString, Rect(this.prSplit + 8, y, 40, ch), FxPalette.smallFont, FxPalette.dim);
			Pen.fillColor = FxPalette.control;
			Pen.fillOval(Rect(m.left - 14, y + (ch * 0.5) - 4, 8, 8));
			Pen.fillColor = FxPalette.fade(FxPalette.accent, light.abs.clip(0, 1));
			Pen.fillOval(Rect(m.left - 14, y + (ch * 0.5) - 4, 8, 8));
			dests.do { |dest, col|
				var r = Rect(m.left + (col * cw) + 1, y + 1, cw - 2, ch - 2);
				var v = unit.getMod(src, dest);
				Pen.fillColor = FxPalette.bg;
				Pen.addRoundedRect(r, 2, 2);
				Pen.fill;
				Pen.fillColor = FxPalette.fade(FxPalette.waveDim, 0.7);
				Pen.fillRect(Rect(r.left + 4, r.center.y, r.width - 8, 1));
				if (v != 0) {
					Pen.fillColor = FxPalette.accent;
					if (v > 0) {
						Pen.fillRect(Rect(r.left + 3, r.center.y - (v * r.height * 0.45), r.width - 6, v * r.height * 0.45));
					} {
						Pen.fillRect(Rect(r.left + 3, r.center.y, r.width - 6, v.neg * r.height * 0.45));
					};
				};
			};
		};
	}

	prMouseDown { |x, y, mod, count|
		var key = this.prCell(x, y);
		dragShift = mod.isShift;
		if (key.notNil) {
			if (count == 2) { unit.set(key, 0); ^this };
			dragKey = key;
			dragFrom = unit.get(key);
			dragY = y;
			^this
		};
		3.do { |i|
			if (this.prCurveRect(i).containsPoint(x @ y)) {
				if (focus != i) { focus = i; focusAction.value(i); this.refresh };
				dragKey = ("drive" ++ (i + 1)).asSymbol;
				dragFrom = unit.get(dragKey);
				dragY = y;
			};
		};
	}

	prMouseMove { |x, y, mod|
		var e, range;
		if (dragKey.isNil) { ^this };
		if (mod.isShift != dragShift) { dragShift = mod.isShift; dragFrom = unit.get(dragKey); dragY = y };
		e = Growl.param(dragKey);
		range = e[\spec].maxval - e[\spec].minval;
		unit.set(dragKey, dragFrom + ((dragY - y) / 200 * range * if (dragShift) { 0.1 } { 1 }));
	}

	prMouseUp { dragKey = nil }

	prWheel { |x, y, delta|
		3.do { |i|
			if (this.prCurveRect(i).containsPoint(x @ y)) {
				var key = ("bias" ++ (i + 1)).asSymbol;
				unit.set(key, unit.get(key) + (delta.sign * 0.02));
			};
		};
	}

	hint {
		^"curves: click one to show its stage, drag for its drive (shift: fine), the wheel for its bias"
		" - matrix: drag a cell for how much a source moves a destination, double click to clear it"
	}
}
