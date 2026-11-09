// =====================================================================
// Growl - three stages of saturation, routed, fed back and modulated
//
// A colouring distortion in the spirit of Ableton's Roar. Each stage is a
// shaper - ten curves, from a gentle tanh to bit reduction and fractal folding
// - and a filter after it. The stages can run one after another, side by side,
// on three frequency bands, or on the middle and the sides; the whole network
// can feed back through a delay; and four sources move eight destinations
// through a matrix.
//
//   Ndef(\bass, { Growl.ar(LFSaw.ar([55, 55.3]) * 0.3, \growl) }).play;
//   g = Growl(Ndef(\bass), \growl);
//   g.gui;
//   g.preset = 'bent fifths';
//   g.routing = \multiband;
//   g.type1 = \fold; g.drive1 = 18; g.filter1 = \lp; g.freq1 = 900;
//   g.setMod(\lfo1, \freq1, 0.4);        // the first lfo moves stage 1's filter
//
//   g = Growl.onBus(0, 2).enabled_(true);  // a node of its own, on the main out
//
// The curves are functions that run on numbers as well as on signals, so the
// window draws the same arithmetic the server runs (*shape, Growl:*shapeSignal).
// Two types do something a curve cannot show: crush also drops the sample rate,
// and noise adds noise that follows the signal.
//
// There is no oversampling - SuperCollider has none built in - so high drive on
// a bright signal aliases; a filter before Growl, or the stage's own low pass
// in a later stage, is the remedy, and sometimes aliasing is what you came for.
// =====================================================================

Growl : FxParamUnit {

	*controlName { ^\growl }

	*shapes { ^#[\soft, \sine, \fold, \clip, \diode, \tube, \rect, \crush, \fractal, \noise] }
	*filters { ^#[\off, \lp, \hp, \bp, \notch, \comb, \ladder] }
	*routings { ^#[\single, \serial, \parallel, \multiband, \midside] }
	*sources { ^#[\lfo1, \lfo2, \env, \noise] }
	*dests { ^#[\drive1, \drive2, \drive3, \freq1, \freq2, \freq3, \feedback, \tone] }

	*hasTail { ^true }
	*displayClass { ^GrowlView }
	*windowSize { ^Point(1000, 580) }

	*modKey { |source, dest| ^("m_" ++ source ++ "_" ++ dest).asSymbol }

	*params {
		// labels, sections, sizes and boxes are the window's (sc-fx-ui, 2026-11-08);
		// the order is the nodeMaps' - append, never reorder
		var stages = [1, 2, 3].collect { |i|
			var section = ("stage " ++ i).asSymbol;
			[
				this.menu(("type" ++ i).asSymbol, this.shapes, #[\soft, \fold, \sine][i - 1], "shaper",
					"which curve: the display draws it", section),
				this.knob(("drive" ++ i).asSymbol, 0, 48, \lin, 12, "dB", "drive",
					"how hard the signal hits the curve - or drag the curve up and down", section: section, dec: 1,
					size: \big),
				this.knob(("bias" ++ i).asSymbol, -1, 1, \lin, 0, "", "bias",
					"shifts the signal along the curve: asymmetry, and so even harmonics - or the wheel on the curve",
					section: section, box: \pct, size: \small),
				this.menu(("filter" ++ i).asSymbol, this.filters, \off, "filter",
					"the filter after the shaper", section),
				this.knob(("freq" ++ i).asSymbol, 20, 18000, \exp, 1800, "Hz", "freq",
					"the filter's frequency - or the comb's pitch", section: section, dec: 0),
				this.knob(("res" ++ i).asSymbol, 0, 1, \lin, 0.3, "", "res",
					"the filter's resonance - or how long the comb rings", section: section, box: \pct, size: \small),
				this.knob(("level" ++ i).asSymbol, -36, 12, \lin, if (i == 1) { -6 } { -12 }, "dB", "level",
					"what comes out of this stage", section: section, dec: 1, bipolar: false)
			]
		};
		var matrix = this.sources.collect { |source|
			this.dests.collect { |dest|
				this.knob(this.modKey(source, dest), -1, 1, \lin, 0, "", nil, "", lag: 0.02, section: nil)
			}
		};
		^[
			this.knob(\input, -24, 24, \lin, 0, "dB", "input", "the level into the stages", section: \input, dec: 1),
			this.menu(\routing, this.routings, \single, "routing",
				"single: one stage; serial: 1 then 2 then 3; parallel: all three on the input;"
				" multiband: low, mid, high; midside: 1 on the middle, 2 on the sides", \input),
			this.knob(\xLow, 40, 1000, \exp, 200, "Hz", "x low", "multiband: between stage 1 and stage 2",
				section: \input, dec: 0, size: \small),
			this.knob(\xHigh, 800, 12000, \exp, 2500, "Hz", "x high", "multiband: between stage 2 and stage 3",
				section: \input, dec: 0, size: \small),
			this.knob(\tone, 200, 20000, \exp, 20000, "Hz", "tone", "a low pass over everything that comes out",
				section: \output, dec: 0),
			this.knob(\squash, 0, 1, \lin, 0, "", "squash", "a compressor over everything that comes out: more is flatter",
				section: \output, box: \pct),
			stages,
			this.knob(\feedback, 0, 1.1, \lin, 0, "", "amount",
				"how much goes round again, whatever the drive: under 100 it dies away, past it sustains on its own",
				section: \feedback, box: \pct, size: \big),
			this.knob(\fbTime, 0.002, 1, \exp, 0.03, "s", "time", "the delay in the loop: short pitches, long echoes",
				box: \ms, lag: 0.2, section: \feedback),
			this.knob(\fbTone, 100, 20000, \exp, 6000, "Hz", "fb tone", "a low pass in the loop", section: \feedback, dec: 0),
			this.knob(\fbGate, -96, 0, \lin, -96, "dB", "gate",
				"the loop closes when the input falls under this - so a sustaining loop still stops",
				section: \feedback, dec: 0, floorText: "off"),
			this.mixParams(\output),
			this.menu(\lfo1Shape, FxLfo.shapes, \sine, "shape", "the first lfo's shape", 'lfo 1'),
			this.knob(\lfo1Rate, 0.01, 30, \exp, 1, "Hz", "rate", "the first lfo's speed", section: 'lfo 1'),
			this.menu(\lfo2Shape, FxLfo.shapes, \sampleHold, "shape", "the second lfo's shape", 'lfo 2'),
			this.knob(\lfo2Rate, 0.01, 30, \exp, 4, "Hz", "rate", "the second lfo's speed", section: 'lfo 2'),
			this.knob(\envGain, -12, 36, \lin, 12, "dB", "gain", "how much the envelope follower is given",
				section: \envelope, dec: 0, bipolar: false),
			this.knob(\envRel, 0.01, 2, \exp, 0.15, "s", "release", "how quickly the follower falls", box: \ms,
				section: \envelope),
			this.knob(\noiseRate, 0.1, 100, \exp, 8, "Hz", "speed", "the speed of the noise source", section: \noise),
			matrix,
			// appended 2026-11-08: the lfos on Tidal's cycle (FxLfo's sync)
			this.menu(\lfo1Sync, FxLfo.syncNames, \free, "sync",
				"lfo 1 free, at its rate - or one period every so many cycles of Tidal's clock", 'lfo 1'),
			this.menu(\lfo2Sync, FxLfo.syncNames, \free, "sync",
				"lfo 2 free, at its rate - or one period every so many cycles of Tidal's clock", 'lfo 2')
		]
	}

	*presetList {
		^[
			\default, (),
			'warm tape', (type1: \tube, drive1: 9, bias1: 0.1, filter1: \lp, freq1: 9000, res1: 0.1, level1: -3, squash: 0.2),
			'bent fifths', (routing: \parallel, type1: \sine, drive1: 18, type2: \fold, drive2: 24, type3: \fractal, drive3: 12,
				filter2: \bp, freq2: 700, res2: 0.6, level1: -12, level2: -12, level3: -18),
			'three bands', (routing: \multiband, type1: \tube, drive1: 12, type2: \diode, drive2: 18, type3: \crush, drive3: 18,
				level1: -3, level2: -6, level3: -12, squash: 0.3),
			\snarl, (routing: \serial, type1: \clip, drive1: 24, type2: \fold, drive2: 12, type3: \soft, drive3: 6,
				filter1: \bp, freq1: 1200, res1: 0.5, filter3: \lp, freq3: 4000, level1: -6, level2: -12, level3: -6,
				squash: 0.4, m_env_freq1: 0.5),
			'wide sides', (routing: \midside, type1: \soft, drive1: 3, level1: -1, type2: \fractal, drive2: 24, level2: -8,
				filter2: \hp, freq2: 900),
			\wasp, (type1: \crush, drive1: 30, filter1: \bp, freq1: 2200, res1: 0.7, level1: -6, m_lfo1_freq1: 0.25, lfo1Rate: 5),
			'howl round', (routing: \serial, type1: \soft, drive1: 18, type2: \diode, drive2: 12, filter2: \comb, freq2: 110, res2: 0.8,
				level1: -6, level2: -6, level3: -36, feedback: 1.04, fbTime: 0.045, fbTone: 2500, fbGate: -48,
				m_lfo1_freq2: 0.15, lfo1Rate: 0.3),
			'dirty dub', (type1: \tube, drive1: 18, filter1: \lp, freq1: 2000, level1: -9, feedback: 0.8, fbTime: 0.375, fbTone: 1800,
				m_noise_feedback: 0.1, noiseRate: 1, mix: 0.6)
		]
	}

	// ----------------------------------------------------------- the curves

	// One function per shape, on the signal already driven and biased. They run
	// on Floats and on UGens alike: what the window draws is what the server does.
	*shapeFunctions {
		^[
			{ |x, db| x.tanh },
			{ |x, db| (x * 0.5pi).sin },
			{ |x, db| x.fold2(1) },
			{ |x, db| x.clip2(1) },
			{ |x, db| (1 - x.max(0).neg.exp) - (0.4 * (1 - (x.min(0) * 2.5).exp)) },
			{ |x, db| (x + (0.25 * x.squared)).tanh },
			{ |x, db| x.tanh.max(0) },
			{ |x, db|
				var levels = 2 ** (12 - (db * 0.2)).max(1);
				(x.tanh * levels).round(1) / levels
			},
			{ |x, db|
				var sum = 0;
				4.do { |k| sum = sum + ((x * 0.5pi * (2 ** k)).sin * (0.5 ** k)) };
				sum * 0.533
			},
			{ |x, db| x.tanh }
		]
	}

	// what a level of x comes out as, through a shape, in numbers
	*shape { |x, type, drive, bias = 0|
		var i = if (type.isNumber) { type.asInteger } { this.shapes.indexOf(type.asSymbol) ? 0 };
		^this.shapeFunctions[i].value((x * drive.dbamp) + bias, drive)
	}

	// the same on one channel of signal, the type as a control
	*shapeSignal { |x, type, drive, bias|
		var y = (x * drive.dbamp) + bias;
		^Select.ar(type, this.shapeFunctions.collect { |f| f.value(y, drive) })
	}

	// ----------------------------------------------------------- the stages

	*prFilter { |x, type, freq, res|
		var rq = (1 - (res * 0.96)).max(0.04);
		var comb = CombC.ar(x, 0.06, (1 / freq).clip(0.0002, 0.05), res.linexp(0, 1, 0.02, 3));
		^Select.ar(type, [
			x,
			RLPF.ar(x, freq, rq),
			RHPF.ar(x, freq, rq),
			BPF.ar(x, freq, rq),
			BRF.ar(x, freq, rq),
			(x + comb) * 0.5,
			MoogFF.ar(x, freq, res * 3.9)
		])
	}

	*prStage { |sig, i, p, driveMod, freqMod|
		var type = p[("type" ++ i).asSymbol];
		var drive = (p[("drive" ++ i).asSymbol] + (driveMod * 24)).clip(0, 60);
		var bias = p[("bias" ++ i).asSymbol];
		var filter = p[("filter" ++ i).asSymbol];
		var freq = (p[("freq" ++ i).asSymbol] * (2 ** (freqMod * 4))).clip(20, 18000);
		var res = p[("res" ++ i).asSymbol];
		var level = p[("level" ++ i).asSymbol].dbamp;
		// crush holds its samples longer, noise is only ever there with its type
		var isCrush = BinaryOpUGen('==', type, 7);
		var isNoise = BinaryOpUGen('==', type, 9);
		var hold = SampleRate.ir / (1 + (isCrush * drive * 0.5));
		^sig.collect { |x|
			var y = this.shapeSignal(x, type, drive, bias);
			y = y + (isNoise * WhiteNoise.ar * x.abs * (drive * 0.04));
			y = (y * (1 - isCrush)) + (Latch.ar(y, Impulse.ar(hold)) * isCrush);
			LeakDC.ar(this.prFilter(y, filter, freq, res)) * level
		}
	}

	// the gain a small signal gets from the input to the output, in dB, by routing
	*prLoopGain { |p|
		var one = [1, 2, 3].collect { |i| p[("drive" ++ i).asSymbol] + p[("level" ++ i).asSymbol] };
		var most = one.reduce(\max);
		^Select.kr(p[\routing], [one[0], one.sum, most, most, one[0].max(one[1])])
	}

	// ------------------------------------------------------------- the graph

	*process { |sig, p, ctx|
		var n = sig.size, source, mods, modOf;
		var lfo1 = FxLfo.kr(p[\lfo1Shape], p[\lfo1Rate], 0, p[\lfo1Sync]);
		var lfo2 = FxLfo.kr(p[\lfo2Shape], p[\lfo2Rate], 0, p[\lfo2Sync]);
		var env = (Amplitude.kr(sig.sum / n, 0.005, p[\envRel]) * p[\envGain].dbamp).clip(0, 1);
		var noise = LFNoise2.kr(p[\noiseRate]);
		var loop = FxLoop(n, 1.1);
		var routing = p[\routing], fb, x, mid, side, low, high, band;
		var s1, s2, s3, in1, in2, in3, out, gate, loopGain;

		source = [lfo1, lfo2, env, noise];
		mods = this.dests.collect { |dest|
			this.sources.collect { |src, i| source[i] * p[this.modKey(src, dest)] }.sum
		};
		modOf = { |dest| mods[this.dests.indexOf(dest)] };

		// all the way down the gate is off, even for true silence
		gate = Lag.kr((Amplitude.kr(sig.sum / n, 0.005, 0.1) > p[\fbGate].dbamp)
			+ (p[\fbGate] <= -95), 0.05).clip(0, 1);
		fb = loop.read(p[\fbTime]);
		fb = fb * (p[\feedback] + modOf.(\feedback)).clip(0, 1.2) * gate;
		x = (sig * p[\input].dbamp) + fb;

		// one set of stages; the routing only says what each of them hears
		mid = x.sum / n;
		side = x - mid;
		// Linkwitz-Riley: two cuts in a row, split low first and the rest again.
		// Subtracting two bands from the input to get the third would leak: their
		// phase has turned by then, and what is left is not the middle
		low = LPF.ar(LPF.ar(x, p[\xLow]), p[\xLow]);
		high = HPF.ar(HPF.ar(x, p[\xLow]), p[\xLow]);
		band = LPF.ar(LPF.ar(high, p[\xHigh]), p[\xHigh]);
		high = HPF.ar(HPF.ar(high, p[\xHigh]), p[\xHigh]);
		in1 = n.collect { |c| Select.ar(routing, [x[c], x[c], x[c], low[c], mid]) };
		s1 = this.prStage(in1, 1, p, modOf.(\drive1), modOf.(\freq1));
		in2 = n.collect { |c| Select.ar(routing, [DC.ar(0), s1[c], x[c], band[c], side[c]]) };
		s2 = this.prStage(in2, 2, p, modOf.(\drive2), modOf.(\freq2));
		in3 = n.collect { |c| Select.ar(routing, [DC.ar(0), s2[c], x[c], high[c], DC.ar(0)]) };
		s3 = this.prStage(in3, 3, p, modOf.(\drive3), modOf.(\freq3));
		out = n.collect { |c|
			Select.ar(routing, [
				s1[c], s3[c],
				(s1[c] + s2[c] + s3[c]) * 0.577,
				s1[c] + s2[c] + s3[c],
				s1[c] + s2[c]
			])
		};

		out = LPF.ar(out, (p[\tone] * (2 ** (modOf.(\tone) * 4))).clip(100, 20000));
		out = Compander.ar(out, out, p[\squash].linlin(0, 1, 1, 0.1), 1,
			p[\squash].linexp(0, 1, 1, 0.15), 0.004, 0.12);
		out = out + (p[\squash] * 0.5 * out);
		// the stages' own gain is taken back out of the loop, whichever of them the
		// routing has it pass through: under 100 it always dies away, over 100 it
		// sustains, whatever the drive. Bypassed, it fades, or over 100 never would
		loopGain = this.prLoopGain(p);
		loop.write((LPF.ar(out, p[\fbTone]) * loopGain.neg.dbamp).tanh * Lag.kr(ctx[\on], 1));
		ctx[\extras] = [lfo1, lfo2, env, noise];
		^out
	}

	// ------------------------------------------------------------ the matrix

	// how much a source moves a destination, -1..1
	setMod { |source, dest, amount| this.set(this.class.modKey(source, dest), amount) }
	getMod { |source, dest| ^this.get(this.class.modKey(source, dest)) }

	clearMods {
		this.class.sources.do { |s|
			this.class.dests.do { |d| values[this.class.modKey(s, d)] = 0 };
		};
		this.prPush;
		this.prChanged(\settings);
	}
}
