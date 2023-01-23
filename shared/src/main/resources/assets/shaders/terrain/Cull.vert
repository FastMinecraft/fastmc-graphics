#version 460

layout(std140) uniform Global {
    mat4 projection;
    mat4 modelView;
    mat4 inverseProjection;
    mat4 inverseModelView;
    vec2 screenResolution;
    float partialTicks;
};

uniform vec3 regionOffset;

layout(location = 0) in vec3 pos;
layout(location = 1) in int vertRenderChunkIndex;
layout(location = 2) in vec3 boxOffset;
layout(location = 3) in vec3 boxSize;

flat out int renderChunkIndex;

const ivec3 shiftVec = ivec3(4, 8, 0);
const ivec3 maskVec = ivec3(15, 16777215, 15);

void main() {
    renderChunkIndex = vertRenderChunkIndex;
//    ivec3 chunkOffset = ivec3(renderChunkIndex) >> shiftVec & maskVec;
//    vec3 renderPos = pos * (boxSize * 14.166667) + (boxOffset * 14.166667);
    vec3 renderPos = pos * 17 - 0.5;

//    renderPos += vec3(chunkOffset << 4) + regionOffset;
    renderPos += boxOffset * 16 + regionOffset;
    gl_Position = projection * modelView * vec4(renderPos, 1.0);
}
