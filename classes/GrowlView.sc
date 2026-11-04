// =====================================================================
// GrowlView - Growl's display: its three curves and its modulation matrix
//
// Left, one transfer curve per stage: input along, output up, the same
// arithmetic the server runs. A stage's routing has silenced is dimmed.
//
//   drag a curve up and down   that stage's drive
//   the wheel on a curve       its bias
//
// Right, the matrix: a row per source - each with a light showing what it is
// doing now - and a column per destination.
//
//   drag a cell up and down    how much that source moves that destination
//   double click a cell        none
// =====================================================================

GrowlView : FxParamView {
	var dragKey, dragY, dragFrom;

	*minHeight { ^190 }

	prCurveRect { |i|
		var w = (bw * 0.5 - 8) / 3, h = bh - 24;
		^Rect(4 + (i * (w + 4)), 18, w, h)
	}

	prMatrixRect { ^Rect(bw * 0.5 + 48, 18, bw * 0.5 - 54, bh - 24) }

	prCell { |x, y|
		var m = this.prMatrixRect, sources = Growl.sources, dests = Growl.dests, col, row;
		if (m.containsPoint(x @ y).not) { ^nil };
		col = ((x - m.left) / m.width * dests.size).floor.asInteger.clip(0, dests.size - 1);
		row = ((y - m.top) / m.height * sources.size).floor.asInteger.clip(0, sources.size - 1);
		^Growl.modKey(sources[row], dests[col])
	}

	prStageActive { |i|
		^switch (unit.routing,
			\single, { i == 0 },
			\midside, { i < 2 },
			{ true }
		)
	}

	prDraw {
		3.do { |i| this.prDrawCurve(i) };
		this.prDrawMatrix;
	}

	prDrawCurve { |i|
		var r = this.prCurveRect(i), n = i + 1;
		var type = unit.get(("type" ++ n).asSymbol), drive = unit.get(("drive" ++ n).asSymbol);
		var bias = unit.get(("bias" ++ n).asSymbol);
		var on = this.prStageActive(i);
		var level = unit.inLevel.clip(0, 1);
		var yOf = { |v| r.top + (r.height * 0.5) - (v.clip(-1.2, 1.2) * r.height * 0.42) };
		Pen.fillColor = FxPalette.bg;
		Pen.fillRect(r);
		Pen.strokeColor = FxPalette.waveDim;
		Pen.width = 1;
		Pen.line(r.left @ yOf.(0), r.right @ yOf.(0));
		Pen.line(r.center.x @ r.top, r.center.x @ r.bottom);
		Pen.stroke;
		Pen.strokeColor = if (on) { FxPalette.eqBand(i * 2) } { FxPalette.waveDim };
		Pen.width = 1.5;
		(r.width.asInteger.max(2)).do { |px|
			var x = px / (r.width - 1) * 2 - 1;
			var p = (r.left + px) @ yOf.(Growl.shape(x, type, drive, bias));
			if (px == 0) { Pen.moveTo(p) } { Pen.lineTo(p) };
		};
		Pen.stroke;
		// the input's level, both ways: how much of the curve is being used
		if (on and: { level > 0.001 }) {
			Pen.fillColor = FxPalette.accent.copy.alpha_(0.14);
			Pen.fillRect(Rect(r.center.x - (level * r.width * 0.5), r.top, level * r.width, r.height));
		};
		this.prCaption("% %".format(n, type), r.left, 4, if (on) { FxPalette.text } { FxPalette.dim });
	}

	prDrawMatrix {
		var m = this.prMatrixRect, sources = Growl.sources, dests = Growl.dests;
		var cw = m.width / dests.size, ch = m.height / sources.size;
		var live = unit.extras ? [];
		sources.do { |src, row|
			var y = m.top + (row * ch);
			var light = live[row] ? 0;
			Pen.fillColor = FxPalette.accent.copy.alpha_(light.abs.clip(0, 1));
			Pen.fillOval(Rect(m.left - 12, y + (ch * 0.5) - 4, 8, 8));
			Pen.font = FxPalette.smallFont;
			Pen.color = FxPalette.dim;
			Pen.stringAtPoint(src.asString, (m.left - 44) @ (y + (ch * 0.5) - 6));
			dests.do { |dest, col|
				var r = Rect(m.left + (col * cw) + 1, y + 1, cw - 2, ch - 2);
				var v = unit.getMod(src, dest);
				Pen.fillColor = FxPalette.bg;
				Pen.fillRect(r);
				if (v != 0) {
					Pen.fillColor = if (v > 0) { FxPalette.wave } { FxPalette.accent };
					if (v > 0) {
						Pen.fillRect(Rect(r.left, r.center.y - (v * r.height * 0.5), r.width, v * r.height * 0.5));
					} {
						Pen.fillRect(Rect(r.left, r.center.y, r.width, v.neg * r.height * 0.5));
					};
				};
			};
		};
		dests.do { |dest, col|
			this.prCaption(dest.asString, m.left + (col * cw) + 2, 4);
		};
	}

	prMouseDown { |x, y, mod, count|
		var key = this.prCell(x, y);
		if (key.notNil) {
			if (count == 2) { unit.set(key, 0); ^this };
			dragKey = key;
			dragFrom = unit.get(key);
			dragY = y;
			^this
		};
		3.do { |i|
			if (this.prCurveRect(i).containsPoint(x @ y)) {
				dragKey = ("drive" ++ (i + 1)).asSymbol;
				dragFrom = unit.get(dragKey);
				dragY = y;
			};
		};
	}

	prMouseMove { |x, y, mod|
		var e, range;
		if (dragKey.isNil) { ^this };
		e = Growl.param(dragKey);
		range = e[\spec].maxval - e[\spec].minval;
		unit.set(dragKey, dragFrom + ((dragY - y) / 100 * range * if (e[\section].isNil) { 0.5 } { 0.5 }));
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
		^"three curves: drag one for its drive, the wheel for its bias - the matrix: drag a cell"
		" to say how much a source moves a destination, double click to clear it"
	}
}
