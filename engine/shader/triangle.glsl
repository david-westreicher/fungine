#version 330 core
layout(location = 0) in vec3 vertex;
uniform mat4 modelviewprojection;

void main(){
    gl_Position = modelviewprojection*vec4(vertex,1.0);
}

//fragment
#version 330 core
out vec3 color;
 
void main(){
    color = vec3(1,1,0);
}