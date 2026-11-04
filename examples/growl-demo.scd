// Growl - a walkthrough, one block at a time
//
// Evaluate each ( ... ) block in turn with the cursor inside it. This is
// an editor document, not a file to .load.

// ---- 1. a sound, and Growl driven from its Ndef
(
s.boot;
)

(
Ndef(\bass, { Growl.ar(LFSaw.ar([55, 55.3]) * 0.3, \growl) }).play;
~g = Growl(Ndef(\bass), \growl);
~g.gui;
)

// ---- 2. settings: each is a property, each moves the window
(
~g.preset = \snarl;
~g.routing = \multiband;
~g.type1 = \fold; ~g.drive1 = 18;
~g.setMod(\lfo1, \freq1, 0.3);    // the first lfo moves stage 1's filter
)

// ---- 3. every preset; evaluate one at a time
~g.preset = \default;
~g.preset = 'warm tape';
~g.preset = 'bent fifths';
~g.preset = 'three bands';
~g.preset = \snarl;
~g.preset = 'wide sides';
~g.preset = \wasp;
~g.preset = 'howl round';
~g.preset = 'dirty dub';

// ---- 4. settings kept, and carried
(
~g.saveAsPreset("demo");
~g.copySettings;
Growl.presetNames.postln;
Growl.removePreset("demo");
)

// ---- 5. the same effect with a node of its own, on the main out
(
~own = Growl.onBus(0, 2);
~own.pasteSettings;
~own.enabled = true;
~own.gui;
)

(
~own.free;
)

// ---- 6. tidy up
(
~g.free;
Ndef(\bass).clear;
)
