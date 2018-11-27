package cn.yiiguxing.tool.dcmviewer.util

import org.dcm4che3.data.*

val SpecificCharset: SpecificCharacterSet = SpecificCharacterSet.valueOf("GBK")

data class AttributeItem(
    val tag: Int,
    val vr: VR,
    val description: String,
    val value: Any,
    val valueString: String,
    val children: List<AttributeItem> = emptyList()
)

fun Attributes.getGBKString(tag: Int, vr: VR): Any {
    return vr.toStrings(getBytes(tag), bigEndian(), SpecificCharset).toString()
}

val Attributes.items: List<AttributeItem>
    get() {
        val sb = StringBuilder()
        val items = ArrayList<AttributeItem>(size())
        val isBigEndian = bigEndian()
        val visitor = Attributes.Visitor { _, tag, vr, value ->
            sb.clear()
            val valueString = when {
                vr.isInlineBinary -> "Binary data"
                vr.prompt(value, isBigEndian, SpecificCharset, 50, sb) -> sb.toString()
                else -> ""
            }
            val description = ElementDictionary.keywordOf(tag, null).takeIf { it.isNotEmpty() } ?: "PrivateTag"
            val children = if (value is Sequence) {
                value.map { it.items }.flatten()
            } else {
                emptyList()
            }

            items += AttributeItem(tag, vr, description, value, valueString, children)
            true
        }
        accept(visitor, false)
        return items.toList()
    }