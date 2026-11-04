# Growl

**Three stages of saturation that can be routed, fed back and modulated - a colouring distortion in the spirit of Ableton's Roar, running anywhere a UGen does, with a window.** A SuperCollider extension.

```supercollider
s.boot;
Ndef(\bass, { Growl.ar(LFSaw.ar([55, 55.3]) * 0.3, \growl) }).play;

g = Growl(Ndef(\bass), \growl);
g.gui;
g.preset = \snarl;
g.routing = \multiband;
g.type1 = \fold; g.drive1 = 18;
g.setMod(\lfo1, \freq1, 0.3);    // the first lfo moves stage 1's filter
```

Every effect of this family lives in the same three places. Write `Growl.ar` into a graph of your own and drive it from the object, as above; give it a node of its own on a bus, `Growl.onBus(0, 2).enabled_(true)`; or put it in a chain, `FxChain.master.add(Growl())`. If SuperDirtGUI is installed, it is an insert on an orbit too: `~sdg.addInsert(0, \growl)`.

It is built on [FxKit](../sc%20fx%20kit), which is built on [ParaEQ](../sc%20EQ), and the class library will not compile without them; the installer links both when the folders sit side by side. It uses SuperCollider's own UGens, not sc3-plugins.

Written for SuperCollider 3.13 on macOS. Nothing in it is platform specific apart from the symlink in `install.scd`, which Linux shares and Windows does not.

## Install

### As a quark

```supercollider
Quarks.install("https://github.com/fracnesco/sc-growl.git");
thisProcess.recompile;
```

The manifest names `sc-fx-kit` as a dependency, which names `sc-eq`, so `Quarks` fetches those first. The quark goes by the name of the repository, `sc-growl`; the class inside it is `Growl`.

### From a clone

Clone `sc-eq` into a folder called `sc EQ`, `sc-fx-kit` into `sc fx kit` and this into `sc growl`, side by side. Open `install.scd` and evaluate its first block, then recompile the class library. It links what is missing and tells you what it did.

On Windows there is no `ln -s`, so use the quark, or copy the three folders into the extensions folder by hand: `Platform.userExtensionDir.openOS` opens it.

Install it one way or the other, **not both**: two copies on the class path is a duplicate class error at compile time.

### Check that it took

```supercollider
Growl.presetNames;
Growl.onBus(0, 2).enabled_(true).gui;
```

Select `Growl` and press cmd/ctrl + d for the help file, and see [examples/growl-demo.scd](examples/growl-demo.scd) for a walkthrough you can evaluate a block at a time.

## What it does

Each stage is a shaper and a filter after it. There are ten shapers: soft, sine, fold, clip, diode, tube, rect, crush, fractal, noise. The window draws each one with the same functions the server runs, so what you see is the curve you get; `Growl.shape` gives you the numbers, and that is the one thing this extension has to get right. Two types do something a curve cannot show: crush also drops the sample rate, and noise adds noise that follows the signal.

The routing says what the stages hear. `single` uses stage 1. `serial` runs them one after another. `parallel` puts all three on the input. `multiband` gives them a low, a middle and a high band, split by Linkwitz-Riley crossovers at `xLow` and `xHigh`. `midside` puts stage 1 on the middle and stage 2 on the sides.

Feedback runs the output back to the input through a delay. The stages' own gain is taken out of the loop, so `feedback` means the same at any drive: under 100% it dies away, and over 100% it sustains until `fbGate` or a bypass closes it.

Four sources (`lfo1`, `lfo2`, `env`, `noise`) move eight destinations (`drive1..3`, `freq1..3`, `feedback`, `tone`) through a matrix. Set it by dragging its cells, or with `setMod`.

There is no oversampling, since SuperCollider has none built in, so high drive on a bright signal aliases. It costs about 4% of one core on an M4 Pro at stereo, the most of this family: all ten shapers and seven filters run on all three stages, whichever you have selected.

It needs more than a stock server's 64 wire buffers above 4 channels: `s.options.numWireBufs = 256` before booting.

## The window

`g.gui` opens the top row (on, presets, save, copy, paste), the display, and a row of knobs per section. Hover over anything and the bar at the bottom says what it does.

In the display, drag a curve up and down for that stage's drive; the wheel on a curve for its bias; drag a matrix cell for how much that source moves that destination; double click a cell to clear it.

## Presets

Built in: default, warm tape, bent fifths, three bands, snarl, wide sides, wasp, howl round, dirty dub.

Yours go in `Platform.userAppSupportDir/growl-presets.scd`: `g.saveAsPreset("mine")`, or type a name in the window and press save. `g.copySettings` and `other.pasteSettings` carry settings from one to another.

## Parameters

Every parameter is a property: `g.input`, `g.input = ...`. These also work by name, `g.set(\input, ...)`, `g.get(\input)`. Knobs clip to their range. This table is generated from the class.

| parameter | section | range | default | what it does |
|---|---|---|---|---|
| `input` | main | -24 .. 24 dB | 0 | the level into the stages |
| `routing` | main | single serial parallel multiband midside | single | single: one stage; serial: 1 then 2 then 3; parallel: all three on the input; multiband: low, mid, high; midside: 1 on the middle, 2 on the sides |
| `xLow` | main | 40 .. 1000 Hz | 200 | multiband: between stage 1 and stage 2 |
| `xHigh` | main | 800 .. 12000 Hz | 2500 | multiband: between stage 2 and stage 3 |
| `tone` | main | 200 .. 20000 Hz | 20000 | a low pass over everything that comes out |
| `squash` | main | 0 .. 1 | 0 | a compressor over everything that comes out: more is flatter |
| `type1` | stage 1 | soft sine fold clip diode tube rect crush fractal noise | soft | which curve: the display draws it |
| `drive1` | stage 1 | 0 .. 48 dB | 12 | how hard the signal hits the curve, in dB |
| `bias1` | stage 1 | -1 .. 1 | 0 | shifts the signal along the curve: asymmetry, and so even harmonics |
| `filter1` | stage 1 | off lp hp bp notch comb ladder | off | the filter after the shaper |
| `freq1` | stage 1 | 20 .. 18000 Hz | 1800 | the filter's frequency - or the comb's pitch |
| `res1` | stage 1 | 0 .. 1 | 0.3 | the filter's resonance - or how long the comb rings |
| `level1` | stage 1 | -36 .. 12 dB | -6 | what comes out of this stage, in dB |
| `type2` | stage 2 | soft sine fold clip diode tube rect crush fractal noise | fold | which curve: the display draws it |
| `drive2` | stage 2 | 0 .. 48 dB | 12 | how hard the signal hits the curve, in dB |
| `bias2` | stage 2 | -1 .. 1 | 0 | shifts the signal along the curve: asymmetry, and so even harmonics |
| `filter2` | stage 2 | off lp hp bp notch comb ladder | off | the filter after the shaper |
| `freq2` | stage 2 | 20 .. 18000 Hz | 1800 | the filter's frequency - or the comb's pitch |
| `res2` | stage 2 | 0 .. 1 | 0.3 | the filter's resonance - or how long the comb rings |
| `level2` | stage 2 | -36 .. 12 dB | -12 | what comes out of this stage, in dB |
| `type3` | stage 3 | soft sine fold clip diode tube rect crush fractal noise | sine | which curve: the display draws it |
| `drive3` | stage 3 | 0 .. 48 dB | 12 | how hard the signal hits the curve, in dB |
| `bias3` | stage 3 | -1 .. 1 | 0 | shifts the signal along the curve: asymmetry, and so even harmonics |
| `filter3` | stage 3 | off lp hp bp notch comb ladder | off | the filter after the shaper |
| `freq3` | stage 3 | 20 .. 18000 Hz | 1800 | the filter's frequency - or the comb's pitch |
| `res3` | stage 3 | 0 .. 1 | 0.3 | the filter's resonance - or how long the comb rings |
| `level3` | stage 3 | -36 .. 12 dB | -12 | what comes out of this stage, in dB |
| `feedback` | feedback | 0 .. 1.1 | 0 | how much goes round again, whatever the drive: under 100 it dies away, past it sustains on its own |
| `fbTime` | feedback | 0.002 .. 1 s | 0.03 | the delay in the loop: short pitches, long echoes |
| `fbTone` | feedback | 100 .. 20000 Hz | 6000 | a low pass in the loop |
| `fbGate` | feedback | -96 .. 0 dB | -96 | the loop closes when the input falls under this - so a sustaining loop still stops |
| `mix` | feedback | 0 .. 1 | 1 | how much of the effect goes out, against the dry signal |
| `output` | feedback | -36 .. 24 dB | 0 | the level of the whole thing, in dB |
| `lfo1Shape` | modulators | sine triangle sawUp sawDown square sampleHold noise | sine | the first lfo's shape |
| `lfo1Rate` | modulators | 0.01 .. 30 Hz | 1 | the first lfo's speed |
| `lfo2Shape` | modulators | sine triangle sawUp sawDown square sampleHold noise | sampleHold | the second lfo's shape |
| `lfo2Rate` | modulators | 0.01 .. 30 Hz | 4 | the second lfo's speed |
| `envGain` | modulators | -12 .. 36 dB | 12 | how much the envelope follower is given |
| `envRel` | modulators | 0.01 .. 2 s | 0.15 | how quickly the follower falls |
| `noiseRate` | modulators | 0.1 .. 100 Hz | 8 | the speed of the noise source |

## Files

| file | what it is |
|---|---|
| [classes/Growl.sc](classes/Growl.sc) | the parameters, the presets, the DSP |
| [classes/GrowlView.sc](classes/GrowlView.sc) | the window's display |
| [HelpSource/Classes/Growl.schelp](HelpSource/Classes/Growl.schelp) | the help file |
| [HelpSource/Classes/GrowlView.schelp](HelpSource/Classes/GrowlView.schelp) | the help file for the display |
| [examples/growl-demo.scd](examples/growl-demo.scd) | a walkthrough, one block at a time |
| [install.scd](install.scd) | the symlink, a check, an uninstall |
| [sc-growl.quark](sc-growl.quark) | the quark manifest |

## License

MIT - see [LICENSE](LICENSE).
