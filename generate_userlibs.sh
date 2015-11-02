#!/usr/bin/env bash
#This just prints the user libraries file that eclipse generated into a file called eclipse.userlibraries
#The only difference is that it substitutes the path to homebrew's libs directory for the prefix to each .jar's path
prefix="$(brew --prefix)/lib"
rm "./eclipse.userlibraries"
echo "Using $prefix as the directory containing the .jar's"
echo '<?xml version="1.0" encoding="UTF-8" standalone="no"?>' >> "./eclipse.userlibraries"
echo '<eclipse-userlibraries version="2">' >> "./eclipse.userlibraries"
echo '    <library name="Utils" systemlibrary="false">' >> "./eclipse.userlibraries"
echo '        <archive javadoc="jar:file:'"$prefix"'/Utils.jar!/doc" path="'"$prefix"'/Utils.jar" source="'"$prefix"'/Utils.jar"/>' >> "./eclipse.userlibraries"
echo '    </library>' >> "./eclipse.userlibraries"
echo '    <library name="Structures" systemlibrary="false">' >> "./eclipse.userlibraries"
echo '        <archive javadoc="jar:file:'"$prefix"'/Structures.jar!/doc" path="'"$prefix"'/Structures.jar" source="'"$prefix"'/Structures.jar"/>' >> "./eclipse.userlibraries"
echo '    </library>' >> "./eclipse.userlibraries"
echo '    <library name="Lexer" systemlibrary="false">' >> "./eclipse.userlibraries"
echo '        <archive javadoc="jar:file:'"$prefix"'/Lexer.jar!/doc" path="'"$prefix"'/Lexer.jar" source="'"$prefix"'/Lexer.jar"/>' >> "./eclipse.userlibraries"
echo '    </library>' >> "./eclipse.userlibraries"
echo '    <library name="JSONLib" systemlibrary="false">' >> "./eclipse.userlibraries"
echo '        <archive javadoc="jar:file:'"$prefix"'/JSONLib.jar!/doc" path="'"$prefix"'/JSONLib.jar" source="'"$prefix"'/JSONLib.jar"/>' >> "./eclipse.userlibraries"
echo '    </library>' >> "./eclipse.userlibraries"
echo '</eclipse-userlibraries>' >> "./eclipse.userlibraries"
echo "Import the eclipse.userlibraries file into Eclipse.  Preferences -> Java -> Build Path -> User Libraries;"
echo "click Import (right side of the window)"
