package util.xml

import java.io.File
import org.redundent.kotlin.xml.parse as nativeParse
import org.redundent.kotlin.xml.node as nativeNodeBuilder
import org.redundent.kotlin.xml.Node as nativeNode
import org.redundent.kotlin.xml.Element as nativeElement
import org.redundent.kotlin.xml.TextElement as nativeTextElement
import org.redundent.kotlin.xml.Comment as nativeComment


fun parse(file: File) = nativeParse(file)
fun parse(string: String) = nativeParse(string)
fun node(name: String, init: Node.() -> Unit) = nativeNodeBuilder(name, init)

typealias Node = nativeNode
typealias Element = nativeElement
typealias TextElement = nativeTextElement
typealias Comment = nativeComment



