import CSwiftJavaJNI

// Swift 6 strict concurrency flags the JNI pointers and handles captured by the
// generated bridging code as non-Sendable. They are raw C handles that the
// Swift-generated glue code passes across task boundaries, so mark them as
// unchecked Sendable to silence the diagnostics.

extension UnsafeMutablePointer: @unchecked Sendable {}
extension UnsafePointer: @unchecked Sendable {}
extension OpaquePointer: @unchecked Sendable {}

extension jobject: @unchecked Sendable {}
extension jclass: @unchecked Sendable {}
extension jstring: @unchecked Sendable {}
extension jarray: @unchecked Sendable {}
