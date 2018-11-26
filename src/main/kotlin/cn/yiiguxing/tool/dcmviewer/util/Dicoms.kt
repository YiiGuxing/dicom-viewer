package cn.yiiguxing.tool.dcmviewer.util

import org.dcm4che3.data.ElementDictionary
import org.dcm4che3.data.SpecificCharacterSet
import org.dcm4che3.data.VR
import org.dcm4che3.imageio.plugins.dcm.DicomMetaData

val SpecificCharset: SpecificCharacterSet = SpecificCharacterSet.valueOf("GBK")

data class AttributeItem(val tag: Int, val vr: VR, val description: String, val value: Any, val valueString: String)

fun DicomMetaData.getAttributesAsGBKString(tag: Int, vr: VR): Any {
    return vr.toStrings(attributes.getBytes(tag), bigEndian(), SpecificCharset).toString()
}

val DicomMetaData.attributeItems: List<AttributeItem>
    get() {
        val sb = StringBuilder()
        val items = ArrayList<AttributeItem>(attributes.size())
        val isBigEndian = bigEndian()
        attributes.accept({ _, tag, vr, value ->
            sb.clear()
            val valueString = when {
                vr.isInlineBinary -> "Binary data"
                vr.prompt(value, isBigEndian, SpecificCharset, 50, sb) -> sb.toString()
                else -> ""
            }
            val description = ElementDictionary.keywordOf(tag, null).takeIf { it.isNotEmpty() } ?: "PrivateTag"
            items += AttributeItem(tag, vr, description, value, valueString)
            true
        }, true)
        return items.toList()
    }