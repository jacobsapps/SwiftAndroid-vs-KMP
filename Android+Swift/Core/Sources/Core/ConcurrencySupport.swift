import CSwiftJavaJNI

extension UnsafeMutablePointer: @unchecked Sendable {}
extension UnsafePointer: @unchecked Sendable {}
extension OpaquePointer: @unchecked Sendable {}

extension jobject: @unchecked Sendable {}
extension jclass: @unchecked Sendable {}
extension jstring: @unchecked Sendable {}
extension jarray: @unchecked Sendable {}
