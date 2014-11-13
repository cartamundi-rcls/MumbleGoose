@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  MumbleGoose startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Add default JVM options here. You can also use JAVA_OPTS and MUMBLE_GOOSE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windowz variants

if not "%OS%" == "Windows_NT" goto win9xME_args
if "%@eval[2+2]" == "4" goto 4NT_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*
goto execute

:4NT_args
@rem Get arguments from the 4NT Shell from JP Software
set CMD_LINE_ARGS=%$

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\MumbleGoose-1.0.jar;%APP_HOME%\lib\commons-io-1.4.jar;%APP_HOME%\lib\disruptor-3.2.1.jar;%APP_HOME%\lib\gson-2.3.jar;%APP_HOME%\lib\guava-17.0.jar;%APP_HOME%\lib\h2-1.4.181.jar;%APP_HOME%\lib\imgscalr-lib-4.2.jar;%APP_HOME%\lib\jEN.jar;%APP_HOME%\lib\jid3lib-0.5.4.jar;%APP_HOME%\lib\joda-time-2.3.jar;%APP_HOME%\lib\jsch-0.1.51.jar;%APP_HOME%\lib\json-simple-1.1.1.jar;%APP_HOME%\lib\json_simple-1.1.jar;%APP_HOME%\lib\jsoup-1.7.3.jar;%APP_HOME%\lib\nfsdb-core-1.0.3.jar;%APP_HOME%\lib\simple-5.1.5.jar;%APP_HOME%\lib\snark.jar;%APP_HOME%\lib\trove4j-3.0.3.jar;%APP_HOME%\lib\twitter4j-async-4.0.2.jar;%APP_HOME%\lib\twitter4j-core-4.0.2.jar;%APP_HOME%\lib\twitter4j-examples-4.0.2.jar;%APP_HOME%\lib\twitter4j-media-support-4.0.2.jar;%APP_HOME%\lib\twitter4j-stream-4.0.2.jar

@rem Execute MumbleGoose
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %MUMBLE_GOOSE_OPTS%  -classpath "%CLASSPATH%" com.melvinkellner.musicplayer.Main %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable MUMBLE_GOOSE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%MUMBLE_GOOSE_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
