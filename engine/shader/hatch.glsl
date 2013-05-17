void main(void)
{
	gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix *gl_Vertex;
	gl_TexCoord[0]= gl_MultiTexCoord0;
	gl_FrontColor = gl_Color;
}

//fragment

uniform sampler2D tex; // 0
/*
uniform float vx_offset=1.0;
uniform float rt_w=1024.0; // GeeXLab built-in
uniform float rt_h=768.0; // GeeXLab built-in
uniform float hatch_y_offset=5.0; // 5.0
uniform float lum_threshold_1=0.5; // 10.0
uniform float lum_threshold_2=0.5; // 10.0
uniform float lum_threshold_3=0.1; // 10.0
uniform float lum_threshold_4=0.1; // 10.0
void main() 
{ 
  vec2 uv = gl_TexCoord[0].xy;
  
  vec3 tc = vec3(1.0, 0.0, 0.0);
    float lum = length(texture2D(tex, uv).rgb);
    tc = vec3(1.0, 1.0, 1.0);
  
    if (lum < lum_threshold_1) 
    {
      if (mod(gl_FragCoord.x + gl_FragCoord.y, 10.0) == 0.0) 
      {
        tc = vec3(0.0, 0.0, 0.0);
      }
    }  
  
    if (lum < lum_threshold_2) 
    {
      if (mod(gl_FragCoord.x - gl_FragCoord.y, 10.0) == 0.0) 
      {
        tc = vec3(0.0, 0.0, 0.0);
      }
    }  
  
    if (lum < lum_threshold_3) 
    {
      if (mod(gl_FragCoord.x + gl_FragCoord.y - hatch_y_offset, 10.0) == 0.0) 
      {
        tc = vec3(0.0, 0.0, 0.0);
      }
    }  
  
    if (lum < lum_threshold_4) 
    {
      if (mod(gl_FragCoord.x - gl_FragCoord.y - hatch_y_offset, 10.0) == 0.0) 
      {
        tc = vec3(0.0, 0.0, 0.0);
      }
    }
    
	gl_FragColor = vec4(tc, 1.0);
}

*/

void main() {
    const float screenWarp_range   = 1.45;

    const vec2    warpCenter = vec2( 0.5, 0.5 );
    vec2    centeredTexcoord = gl_TexCoord[0].xy - warpCenter;

	float	radialLength = length( centeredTexcoord );
	vec2	radialDir = normalize( centeredTexcoord );

	// get it down into the 0 - PI/2 range
	float	range = screenWarp_range;
	float	scaledRadialLength = radialLength * range;
	float	tanScaled = tan( scaledRadialLength );

    float   rescaleValue = tan( 0.5 * range );

    // If radialLength was 0.5, we want rescaled to also come out
    // as 0.5, so the edges of the rendered image are at the edges
    // of the warped image.
	float	rescaled = tanScaled / rescaleValue;

    vec2 warped = warpCenter + vec2( 0.5, 0.5 ) * radialDir * rescaled;

	gl_FragColor = texture2D(tex, warped);
}