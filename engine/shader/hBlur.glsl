void main(void)
{
   gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix *gl_Vertex;
	gl_TexCoord[0]= gl_MultiTexCoord0;
}
//fragment
uniform sampler2D tex; // the texture with the scene you want to blur 
uniform float height;

void main(void)
{
   float blurSize = 1.0/height;
   vec4 sum = vec4(0.0);
 
   vec2 vTexCoord = gl_TexCoord[0].xy;
 
   // blur in y (vertical)
   // take nine samples, with the distance blurSize between them
   sum += texture2D(tex, vec2(vTexCoord.x - 4.0*blurSize, vTexCoord.y)) * 0.05;
   sum += texture2D(tex, vec2(vTexCoord.x - 3.0*blurSize, vTexCoord.y)) * 0.09;
   sum += texture2D(tex, vec2(vTexCoord.x - 2.0*blurSize, vTexCoord.y)) * 0.12;
   sum += texture2D(tex, vec2(vTexCoord.x - blurSize, vTexCoord.y)) * 0.15;
   sum += texture2D(tex, vec2(vTexCoord.x, vTexCoord.y)) * 0.16;
   sum += texture2D(tex, vec2(vTexCoord.x + blurSize, vTexCoord.y)) * 0.15;
   sum += texture2D(tex, vec2(vTexCoord.x + 2.0*blurSize, vTexCoord.y)) * 0.12;
   sum += texture2D(tex, vec2(vTexCoord.x + 3.0*blurSize, vTexCoord.y)) * 0.09;
   sum += texture2D(tex, vec2(vTexCoord.x + 4.0*blurSize, vTexCoord.y)) * 0.05;
 
   gl_FragColor = sum;
}