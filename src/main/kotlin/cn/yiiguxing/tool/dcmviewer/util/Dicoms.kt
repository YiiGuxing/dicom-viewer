package cn.yiiguxing.tool.dcmviewer.util

import org.dcm4che3.data.*
import org.dcm4che3.imageio.plugins.dcm.DicomMetaData
import org.dcm4che3.util.TagUtils

val SpecificCharset: SpecificCharacterSet = SpecificCharacterSet.valueOf("GBK")

data class AttributeItem(
    val tag: Int,
    val vr: VR,
    val description: String,
    val value: Any,
    val tagString: String,
    val vrString: String,
    val valueString: String,
    val children: List<AttributeItem> = emptyList()
)

fun Attributes.getGBKStrings(tag: Int, vr: VR, default: Any): Any {
    return getBytes(tag)?.let { vr.toStrings(it, bigEndian(), SpecificCharset) } ?: default
}

val DicomMetaData.attributeItems: List<AttributeItem>
    get() {
        val fileMetaItems = fileMetaInformation.items
        val attrItems = attributes.items

        return ArrayList<AttributeItem>(fileMetaItems.size + attrItems.size).apply {
            addAll(fileMetaItems)
            addAll(attrItems)
        }.toList()
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

            items += AttributeItem(tag, vr, description, value, TagUtils.toString(tag), vr.name, valueString, children)
            true
        }
        accept(visitor, false)
        return items.toList()
    }