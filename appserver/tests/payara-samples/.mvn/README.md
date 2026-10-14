# .mvn

This directory exists to mark `appserver/tests/payara-samples` as the Maven multi-module root.

Maven sets `maven.multiModuleProjectDirectory` to the directory containing `.mvn`, or to the directory Maven was run from if there isn't one. The poms in this tree build paths from that property (the unpacked server in `payara.home`, the Payara Micro jar and the stashed server logs), so without this directory those paths change depending on where Maven is run. Running a single module would unpack its own copy of the server instead of sharing one.

Do not add a `maven.config` here containing comments. Maven 3.6.x has no comment syntax for `maven.config` and treats every token as a command-line option.
