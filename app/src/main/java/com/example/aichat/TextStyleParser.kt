package com.example.aichat

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan

object TextStyleParser {
    private const val HEADER_SCALE = 1.5f
    private val CODE_COLOR = Color.rgb(40, 44, 52)

    // 严格限制加粗/斜体边界（避免符号粘连干扰）
    private val BOLD_REGEX = """(?<!\w)(\*\*)(?!\s)(.+?)(?<!\s)(\*\*)(?!\w)""".toRegex()
    private val ITALIC_REGEX = """(?<!\w)(\*)(?!\s)(.+?)(?<!\s)(\*)(?!\w)""".toRegex()
    private val HEADER_REGEX = """^(#{1,6})\s*(.+?)(?=\s|$|\n)""".toRegex(RegexOption.MULTILINE)

    fun processContent(text: String): SpannableString {
        val builder = SpannableStringBuilder(text)
        processHeaders(builder)
        processCodeBlocks(builder)
        processFormats(builder, BOLD_REGEX to { s, e -> applyBold(builder, s, e) })
        processFormats(builder, ITALIC_REGEX to { s, e -> applyItalic(builder, s, e) })
        return SpannableString(builder)
    }

    // 标题处理
    private fun processHeaders(builder: SpannableStringBuilder) {
        val matches = HEADER_REGEX.findAll(builder).toList()
        var totalOffset = 0

        matches.forEach { match ->
            val (hashes, content) = match.destructured
            val originalStart = match.groups[1]!!.range.first - totalOffset
            val originalEnd = match.range.last + 1 - totalOffset

            // 删除 # 符号和空格
            val headerSymbolLength = hashes.length + 1 // 包括 # 和空格
            builder.delete(originalStart, originalStart + headerSymbolLength)

            // 设置标题样式
            val spanStart = originalStart
            val spanEnd = spanStart + content.length
            builder.setSpan(StyleSpan(Typeface.BOLD), spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(RelativeSizeSpan(HEADER_SCALE - hashes.length * 0.1f), spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            totalOffset += headerSymbolLength // 累计删除的字符数
        }
    }

    // 加粗/斜体处理
    private fun processFormats(
        builder: SpannableStringBuilder,
        regexToHandler: Pair<Regex, (Int, Int) -> Unit>
    ) {
        val (regex, handler) = regexToHandler
        val matches = regex.findAll(builder).toList()
        var totalOffset = 0

        matches.forEach { match ->
            val (prefix, content, suffix) = match.destructured
            val originalStart = match.groups[1]!!.range.first - totalOffset
            val originalEnd = match.groups[3]!!.range.last + 1 - totalOffset

            // 删除包裹符号（** 或 *）
            builder.delete(originalEnd - suffix.length, originalEnd)
            builder.delete(originalStart, originalStart + prefix.length)

            // 计算新范围
            val newStart = originalStart
            val newEnd = originalEnd - (prefix.length + suffix.length)

            // 应用样式
            handler(newStart, newEnd)

            // 更新全局偏移量
            totalOffset += (prefix.length + suffix.length)
        }
    }

    private fun applyBold(builder: SpannableStringBuilder, start: Int, end: Int) {
        builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun applyItalic(builder: SpannableStringBuilder, start: Int, end: Int) {
        builder.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    // 代码块处理
    private fun processCodeBlocks(builder: SpannableStringBuilder) {
        val codeBlockRegex = """```([\s\S]*?)```""".toRegex()
        val matches = codeBlockRegex.findAll(builder).toList()
        var totalOffset = 0

        matches.forEach { match ->
            val (content) = match.destructured
            val originalStart = match.range.first - totalOffset
            val originalEnd = match.range.last + 1 - totalOffset

            // 删除 ```
            builder.delete(originalEnd - 3, originalEnd)
            builder.delete(originalStart, originalStart + 3)

            // 设置代码样式
            val newEnd = originalEnd - 6
            builder.setSpan(ForegroundColorSpan(CODE_COLOR), originalStart, newEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            totalOffset += 6
        }
    }
}

