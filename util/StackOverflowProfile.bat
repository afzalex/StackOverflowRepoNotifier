@echo off
title StackOverflowProfile
rem Setting Java location to path
pushd %cd%
%homedrive%
if exist "%homedrive%\Program Files\Java\jre*" (
	set loc="%homedrive%\Program Files\Java"
) else (
	if exist "%homedrive%\Program Files (x86)\Java\jre*" (
		set loc="%homedrive%\Program Files (x86)\Java"
	) else (
		(
			echo Java file not found
		)&(
			pause
		)&(
			exit
		)
	)
)
cd %loc%
cd jre*\bin
set path=%path%;%cd%
popd
rem setting Java location complete

java -jar StackOverflowProfile.jar
