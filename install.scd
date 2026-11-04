// Symlink this repository into the SuperCollider extensions folder.
// Evaluate the block below, then recompile the class library
// (Language -> Recompile Class Library, or ctrl/cmd + shift + L).
//
// This is the route for a clone you want to keep editing. If you would
// rather have it installed and updated for you, do not use this file -
// install it as a quark instead, from a fresh sclang:
//
//   Quarks.install("https://github.com/fracnesco/sc-growl.git");
//   thisProcess.recompile;
//
// One or the other, not both: two copies on the class path is a
// duplicate class error at compile time.
//
// ln -s is macOS and Linux. On Windows, use the quark, or copy this
// folder into Platform.userExtensionDir by hand - .openOS opens it.
//
// Growl is built on sc fx kit (../sc fx kit), which is built on ParaEQ
// (../sc EQ), so both have to be installed too. If they are sitting next
// to this folder, this script links them as well.

(
var link, src, parent, dst;

src = PathName(thisProcess.nowExecutingPath).pathOnly;
if (src.endsWith("/")) { src = src.drop(-1) };			// ln -s dislikes it
parent = PathName(src).parentPath;
dst = Platform.userExtensionDir +/+ "Growl";

link = { |from, to, what, done|
	if (from.endsWith("/")) { from = from.drop(-1) };
	case
	{ File.exists(to) } {
		("Growl: % is already installed at %.".format(what, to)).postln;
		done.value;
	}
	{ File.exists(from).not } {
		("Growl: % is not installed, and there is no copy of it at %."
			"\n  The class library will not compile without it."
			"\n  Install it first, then run this again."
			.format(what, from)).warn;
	}
	{ true } {
		File.mkdir(Platform.userExtensionDir);
		("ln -s" + from.shellQuote + to.shellQuote).unixCmd({ |exit|
			if (exit == 0) {
				"Growl: linked % -> %".format(what, to).postln;
				done.value;
			} {
				"Growl: linking % failed with exit code %".format(what, exit).warn;
			};
		});
	};
};

// what it is built on first - without them the class library will not compile
link.value(parent +/+ "sc EQ", Platform.userExtensionDir +/+ "ParaEQ", "ParaEQ", {
	link.value(parent +/+ "sc fx kit", Platform.userExtensionDir +/+ "FxKit", "FxKit", {
		link.value(src, dst, "Growl", {
			"".postln;
			"Now recompile the class library: ctrl/cmd + shift + L".postln;
		});
	});
});
)


// ---------------------------------------------------------------- verify
(
var missing = [\Growl, \GrowlView]
	.select { |sym| sym.asClass.isNil };

if (missing.notEmpty) {
	("Growl is not compiled - % is missing."
		"\n  Recompile the class library, ctrl/cmd + shift + L, and check the"
		"\n  post window for compile errors - a missing ParaEQ or FxKit is the usual reason."
		.format(missing.join(", "))).warn;
} {
	"Growl is installed.".postln;
	"  presets: % - %".format(Growl.presetNames.size, Growl.presetNames.join(", ")).postln;
	"  in a graph:  Growl.ar(sig, \\growl)      then  Growl(Ndef(\\x), \\growl).gui".postln;
	"  on a bus:    Growl.onBus(0, 2).enabled_(true).gui".postln;
	"  an insert:   ~sdg.addInsert(0, \\growl)".postln;
	"  help:        Growl, then cmd+d".postln;
};
)


// ------------------------------------------------------------- uninstall
(
var link = Platform.userExtensionDir +/+ "Growl";
case
{ File.type(link) == \symlink } {
	File.delete(link);
	("Removed %."
		"\n  Recompile the class library to finish.".format(link)).postln;
}
{ File.type(link) == \not_found } {
	"Nothing to remove at %.".format(link).postln;
}
{ true } {
	"% is not a link, leaving it alone.".format(link).warn;
};
)
