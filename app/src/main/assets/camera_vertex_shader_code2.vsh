uniform mat4 uMVPMatrix;
uniform mat4 uSTMatrix;
attribute vec2 aPosition;
attribute vec2 aTextureCoord;
varying vec2 vTextureCoord;





void main() {
    gl_Position = uMVPMatrix * vec4(aPosition.x, aPosition.y, 1.0, 1.0);
    vTextureCoord = (uSTMatrix * vec4(aTextureCoord.x, aTextureCoord.y, 1.0, 1.0)).xy;
}