uniform mat4 uMVPMatrix;
uniform mat4 uTexMatrix;
attribute vec4 aPosition;
attribute vec4 aTextureCoord;
varying vec2 textureCoordinate;
void main() {
    gl_Position = uMVPMatrix * vec4(aPosition.x, aPosition.y, 1.0, 1.0);
    textureCoordinate = (uTexMatrix * vec4(aTextureCoord.x, aTextureCoord.y, 1.0, 1.0)).xy;
}
