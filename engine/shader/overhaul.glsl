#version 330
layout(location = 0) in vec3 position;
layout(location = 1) in vec3 color;
out vec3 col;
uniform mat4 modelviewprojection;
void main()
{
	col = color;
	gl_Position = modelviewprojection*vec4(position,1.0);
}

//fragment
#version 330
in vec3 col;
out vec4 outputColor;
void main()
{
	outputColor = vec4(col, 1.0f);
}