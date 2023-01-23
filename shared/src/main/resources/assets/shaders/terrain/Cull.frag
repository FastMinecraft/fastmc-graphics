#version 460

layout(early_fragment_tests) in;

layout(std430) buffer VisibleBuffer {
    uint visible[];
} visibleBuffer;

flat in int renderChunkIndex;

void main() {
    visibleBuffer.visible[renderChunkIndex] = 1u;
}
