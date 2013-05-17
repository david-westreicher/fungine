void main(void)
{
	gl_Position = gl_ProjectionMatrix*gl_ModelViewMatrix *gl_Vertex; 
	gl_FrontColor = gl_Color;
	gl_TexCoord[0]= gl_MultiTexCoord0;
}

//fragment

uniform sampler2D ambTex;
uniform sampler2D depthTex;
uniform float zNear;
uniform float zFar;
uniform float focus;
uniform float time;
uniform float aspectRatio;
uniform vec3 camPos;

 
 
 float LinearizeDepth(vec2 uv)
{
  float z = texture2D(depthTex, uv).x;
  return (2.0 * zNear) / (zFar + zNear - z * (zFar - zNear));
}
 
 
void main()
{
 		
        vec2 aspectcorrect = vec2(1.0,aspectRatio);
       	vec2 coord = gl_TexCoord[0].xy;
     	float factor = distance(texture2D(depthTex, coord).xyz,camPos)/zFar-focus;
         
        vec2 dofblur = vec2(mix(0,0.05,factor/2)); 
        vec4 col = vec4(0.0);
       
        col += texture2D(ambTex, coord);
        col += texture2D(ambTex,coord + (vec2( 0.0,0.4 )*aspectcorrect) * dofblur);
        col += texture2D(ambTex, coord.xy + (vec2( 0.15,0.37 )*aspectcorrect) * dofblur);
        col += texture2D(ambTex, coord.xy + (vec2( 0.29,0.29 )*aspectcorrect) * dofblur);
        col += texture2D(ambTex, coord.xy + (vec2( -0.37,0.15 )*aspectcorrect) * dofblur);       
        col += texture2D(ambTex, coord.xy + (vec2( 0.4,0.0 )*aspectcorrect) * dofblur);   
        col += texture2D(ambTex, coord.xy + (vec2( 0.37,-0.15 )*aspectcorrect) * dofblur);       
        col += texture2D(ambTex, coord.xy + (vec2( 0.29,-0.29 )*aspectcorrect) * dofblur);       
        col += texture2D(ambTex, coord.xy + (vec2( -0.15,-0.37 )*aspectcorrect) * dofblur);
        col += texture2D(ambTex, coord.xy + (vec2( 0.0,-0.4 )*aspectcorrect) * dofblur); 
        col += texture2D(ambTex, coord.xy + (vec2( -0.15,0.37 )*aspectcorrect) * dofblur);
        col += texture2D(ambTex, coord.xy + (vec2( -0.29,0.29 )*aspectcorrect) * dofblur);
        col += texture2D(ambTex, coord.xy + (vec2( 0.37,0.15 )*aspectcorrect) * dofblur); 
        col += texture2D(ambTex, coord.xy + (vec2( -0.4,0.0 )*aspectcorrect) * dofblur); 
        col += texture2D(ambTex, coord.xy + (vec2( -0.37,-0.15 )*aspectcorrect) * dofblur);       
        col += texture2D(ambTex, coord.xy + (vec2( -0.29,-0.29 )*aspectcorrect) * dofblur);       
        col += texture2D(ambTex, coord.xy + (vec2( 0.15,-0.37 )*aspectcorrect) * dofblur);
       
        col += texture2D(ambTex, coord.xy + (vec2( 0.15,0.37 )*aspectcorrect) * dofblur*0.9);
        col += texture2D(ambTex, coord.xy + (vec2( -0.37,0.15 )*aspectcorrect) * dofblur*0.9);           
        col += texture2D(ambTex, coord.xy + (vec2( 0.37,-0.15 )*aspectcorrect) * dofblur*0.9);           
        col += texture2D(ambTex, coord.xy + (vec2( -0.15,-0.37 )*aspectcorrect) * dofblur*0.9);
        col += texture2D(ambTex, coord.xy + (vec2( -0.15,0.37 )*aspectcorrect) * dofblur*0.9);
        col += texture2D(ambTex, coord.xy + (vec2( 0.37,0.15 )*aspectcorrect) * dofblur*0.9);            
        col += texture2D(ambTex, coord.xy + (vec2( -0.37,-0.15 )*aspectcorrect) * dofblur*0.9);   
        col += texture2D(ambTex, coord.xy + (vec2( 0.15,-0.37 )*aspectcorrect) * dofblur*0.9);   
       
        col += texture2D(ambTex, coord.xy + (vec2( 0.29,0.29 )*aspectcorrect) * dofblur*0.7);
        col += texture2D(ambTex, coord.xy + (vec2( 0.4,0.0 )*aspectcorrect) * dofblur*0.7);       
        col += texture2D(ambTex, coord.xy + (vec2( 0.29,-0.29 )*aspectcorrect) * dofblur*0.7);   
        col += texture2D(ambTex, coord.xy + (vec2( 0.0,-0.4 )*aspectcorrect) * dofblur*0.7);     
        col += texture2D(ambTex, coord.xy + (vec2( -0.29,0.29 )*aspectcorrect) * dofblur*0.7);
        col += texture2D(ambTex, coord.xy + (vec2( -0.4,0.0 )*aspectcorrect) * dofblur*0.7);     
        col += texture2D(ambTex, coord.xy + (vec2( -0.29,-0.29 )*aspectcorrect) * dofblur*0.7);   
        col += texture2D(ambTex, coord.xy + (vec2( 0.0,0.4 )*aspectcorrect) * dofblur*0.7);
                         
        col += texture2D(ambTex, coord.xy + (vec2( 0.29,0.29 )*aspectcorrect) * dofblur*0.4);
        col += texture2D(ambTex, coord.xy + (vec2( 0.4,0.0 )*aspectcorrect) * dofblur*0.4);       
        col += texture2D(ambTex, coord.xy + (vec2( 0.29,-0.29 )*aspectcorrect) * dofblur*0.4);   
        col += texture2D(ambTex, coord.xy + (vec2( 0.0,-0.4 )*aspectcorrect) * dofblur*0.4);     
        col += texture2D(ambTex, coord.xy + (vec2( -0.29,0.29 )*aspectcorrect) * dofblur*0.4);
        col += texture2D(ambTex, coord.xy + (vec2( -0.4,0.0 )*aspectcorrect) * dofblur*0.4);     
        col += texture2D(ambTex, coord.xy + (vec2( -0.29,-0.29 )*aspectcorrect) * dofblur*0.4);   
        col += texture2D(ambTex, coord.xy + (vec2( 0.0,0.4 )*aspectcorrect) * dofblur*0.4);       
                       
        gl_FragColor = (col/41.0);
         //gl_FragColor =texture2D(ambTex, coord.xy);
        //gl_FragColor =vec4(factor);
        //gl_FragColor.a = 1.0;
}

