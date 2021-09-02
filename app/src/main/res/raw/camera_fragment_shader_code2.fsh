#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
uniform int enableBeauty;
uniform highp float brightness;
uniform int width;
uniform int height;
uniform float opacity;

const lowp vec3 whiteFilter = vec3(0.1, 0.1, 0.1);   //save for light whiting

 const lowp vec3 warmFilter = vec3(0.0, 0.78, 0.92);


highp  vec2 blurCoordinates[20];


float hardLight(float color)
{
	if(color <= 0.5)
		color = color * color * 2.0;
	else
		color = 1.0 - ((1.0 - color)*(1.0 - color) * 2.0);
	return color;
}


vec3 light(vec3 color)
{

   return color;
}


mediump vec3 beauty()
{

  vec3 centralColor = texture2D(sTexture, vTextureCoord).rgb;
if(enableBeauty != 0){

       float x_a = float(width);
               float y_a = float(height);

               float mul_x = 2.0 / x_a;
               float mul_y = 2.0 / y_a;
               vec2 blurCoordinates0 = vTextureCoord + vec2(0.0 * mul_x, -10.0 * mul_y);
               vec2 blurCoordinates2 = vTextureCoord + vec2(8.0 * mul_x, -5.0 * mul_y);
               vec2 blurCoordinates4 = vTextureCoord + vec2(8.0 * mul_x, 5.0 * mul_y);
               vec2 blurCoordinates6 = vTextureCoord + vec2(0.0 * mul_x, 10.0 * mul_y);
               vec2 blurCoordinates8 = vTextureCoord + vec2(-8.0 * mul_x, 5.0 * mul_y);
               vec2 blurCoordinates10 = vTextureCoord + vec2(-8.0 * mul_x, -5.0 * mul_y);

               mul_x = 1.8 / x_a;
               mul_y = 1.8 / y_a;
               vec2 blurCoordinates1 = vTextureCoord + vec2(5.0 * mul_x, -8.0 * mul_y);
               vec2 blurCoordinates3 = vTextureCoord + vec2(10.0 * mul_x, 0.0 * mul_y);
               vec2 blurCoordinates5 = vTextureCoord + vec2(5.0 * mul_x, 8.0 * mul_y);
               vec2 blurCoordinates7 = vTextureCoord + vec2(-5.0 * mul_x, 8.0 * mul_y);
               vec2 blurCoordinates9 = vTextureCoord + vec2(-10.0 * mul_x, 0.0 * mul_y);
               vec2 blurCoordinates11 = vTextureCoord + vec2(-5.0 * mul_x, -8.0 * mul_y);



               float central;
               float gaussianWeightTotal;
               float sum;
               float sampler;
               float distanceFromCentralColor;
               float gaussianWeight;

               float distanceNormalizationFactor = 3.6;

               central = texture2D(sTexture, vTextureCoord).g;
               gaussianWeightTotal = 0.2;
               sum = central * 0.2;

               sampler = texture2D(sTexture, blurCoordinates0).g;
               distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
               gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
               gaussianWeightTotal += gaussianWeight;
               sum += sampler * gaussianWeight;

               sampler = texture2D(sTexture, blurCoordinates1).g;
               distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
               gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
               gaussianWeightTotal += gaussianWeight;
               sum += sampler * gaussianWeight;

               sampler = texture2D(sTexture, blurCoordinates2).g;
               distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
               gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
               gaussianWeightTotal += gaussianWeight;
               sum += sampler * gaussianWeight;

               sampler = texture2D(sTexture, blurCoordinates3).g;
               distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
               gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
               gaussianWeightTotal += gaussianWeight;
               sum += sampler * gaussianWeight;

               sampler = texture2D(sTexture, blurCoordinates4).g;
               distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
               gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
               gaussianWeightTotal += gaussianWeight;
               sum += sampler * gaussianWeight;

               sampler = texture2D(sTexture, blurCoordinates5).g;
               distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
               gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
               gaussianWeightTotal += gaussianWeight;
               sum += sampler * gaussianWeight;

               sampler = texture2D(sTexture, blurCoordinates6).g;
               distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
               gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
               gaussianWeightTotal += gaussianWeight;
               sum += sampler * gaussianWeight;

               sampler = texture2D(sTexture, blurCoordinates7).g;
               distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
               gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
               gaussianWeightTotal += gaussianWeight;
               sum += sampler * gaussianWeight;

               sampler = texture2D(sTexture, blurCoordinates8).g;
               distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
               gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
               gaussianWeightTotal += gaussianWeight;
               sum += sampler * gaussianWeight;

               sampler = texture2D(sTexture, blurCoordinates9).g;
               distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
               gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
               gaussianWeightTotal += gaussianWeight;
               sum += sampler * gaussianWeight;

               sampler = texture2D(sTexture, blurCoordinates10).g;
               distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
               gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
               gaussianWeightTotal += gaussianWeight;
               sum += sampler * gaussianWeight;

               sampler = texture2D(sTexture, blurCoordinates11).g;
               distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
               gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
               gaussianWeightTotal += gaussianWeight;
               sum += sampler * gaussianWeight;



               sum = sum/gaussianWeightTotal;

               sampler = centralColor.g - sum + 0.5;

               // 高反差保留
               for(int i = 0; i < 5; ++i) {
                   if(sampler <= 0.5) {
                       sampler = sampler * sampler * 2.0;
                   } else {
                       sampler = 1.0 - ((1.0 - sampler)*(1.0 - sampler) * 2.0);
                   }
               }

               float aa = 1.0 + pow(sum, 0.3) * 0.09;
               vec3 smoothColor = centralColor * aa - vec3(sampler) * (aa - 1.0);
               smoothColor = clamp(smoothColor, vec3(0.0), vec3(1.0));

               smoothColor = mix(centralColor, smoothColor, pow(centralColor.g, 0.33));
               smoothColor = mix(centralColor, smoothColor, pow(centralColor.g, 0.39));

               smoothColor = mix(centralColor, smoothColor, opacity);





       	vec4 back =  vec4(pow(smoothColor, vec3(0.96)), 1.0);

        return back.rgb;
          }else{
                return centralColor.rgb;
            }
}

void main() {
gl_FragColor=vec4(light(beauty()),1.0);
}