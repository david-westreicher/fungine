export LD_LIBRARY_PATH="/home/david/Documents/workspace/fungine/native"
export GALLIUM_HUD=fps,cpu+cpu0+cpu1+cpu2+cpu3:100
export vblank_mode=0

if [ ! -d "build" ]; then
	mkdir build
fi
javac -cp .:libs/jna/*:libs/jogl/*:libs/jama/*:libs/joal/*:libs/gson/*:libs/ode4j/*:libs/vecmath/*:libs/rift/*:libs/jinput/*:libs/stackalloc/* -sourcepath src -d build src/**/*.java src/**/**/*.java

java -Djava.library.path=$LD_LIBRARY_PATH -Djna.library.path=$LIBRARY_PATH -cp build/:libs/jna/*:libs/jogl/*:libs/jama/*:libs/joal/*:libs/gson/*:libs/ode4j/*:libs/vecmath/*:libs/rift/*:libs/jinput/*:libs/stackalloc/* test.OpenGLTest games/engineoverhaul/ engine/engine.cfg 
