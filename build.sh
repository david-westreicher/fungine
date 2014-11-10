export LD_LIBRARY_PATH="native"
export GALLIUM_HUD=fps,cpu+cpu0+cpu1+cpu2+cpu3:100
export vblank_mode=0

if [ ! -d "$LD_LIBRARY_PATH" ]; then
	echo "You have no \"native\" folder! It should contain the dll's/so's for libawesomium and Jinput!"
	exit 0
fi

if [ ! -d "$1" ]; then
	echo "The specified gamefolder \"$1\" doesn't exist!"
	exit 0
fi

if [ ! -d "build" ]; then
	mkdir build
fi

for i in libs/**/*.jar; do
    CLASSPATH=$CLASSPATH:$i
done
CLASSPATH=`echo $CLASSPATH | cut -c2-`

javac -cp $CLASSPATH -sourcepath src -d build src/**/*.java src/**/**/*.java

java -Djava.library.path=$LD_LIBRARY_PATH -Djna.library.path=$LIBRARY_PATH -cp build/:$CLASSPATH test.OpenGLTest $1 engine/engine.cfg 
