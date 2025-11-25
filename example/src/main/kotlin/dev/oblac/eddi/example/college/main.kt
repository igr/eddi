package dev.oblac.eddi.example.college

import com.github.ajalt.colormath.model.Oklab
import com.github.ajalt.colormath.model.SRGB
import com.github.ajalt.colormath.transform.interpolator
import com.github.ajalt.colormath.transform.sequence
import com.github.ajalt.mordant.input.interactiveSelectList
import com.github.ajalt.mordant.rendering.*
import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal

private val shadowColor = rgb("#24218c")
private val green = rgb("#2fb479")
private val blue = rgb("#2f61b4")
private val magenta = rgb("#b02fb4")
private val orange = rgb("#b4832f")

fun main() {
    val t = Terminal(
        theme = Theme {
            styles["hr.rule"] = shadowColor
            styles["panel.border"] = shadowColor
        },
        interactive = true,
        ansiLevel = AnsiLevel.TRUECOLOR
    )

    printHeader(t)
    showMainMenu(t)
}

private fun printHeader(terminal: Terminal) {
    if (!terminal.terminalInfo.interactive) {
        error("Terminal is not interactive")
    }
    val layout = table {
        borderType = BorderType.BLANK
        column(0) { width = ColumnWidth.Fixed(60) }
        body {
            row {
                cell(titleExample()) {
                    columnSpan = 2
                    align = TextAlign.CENTER
                }
            }
        }
    }
    terminal.println("\n\n")
    terminal.println(layout)
}

private const val MENU_ADD_STUDENT = "Add Student"
private const val MENU_LIST_STUDENTS = "List Students"
private const val MENU_EXIT = "Exit"

private fun showMainMenu(terminal: Terminal) {
    val selection = terminal.interactiveSelectList(
        listOf(MENU_ADD_STUDENT, MENU_LIST_STUDENTS, MENU_EXIT),
        title = "Select an option:",
    )

    when (selection) {
        MENU_ADD_STUDENT -> {
            terminal.println(green("Adding a new student... (not implemented)"))
            showMainMenu(terminal)
        }
        MENU_LIST_STUDENTS -> {
            terminal.println(blue("Listing all students... (not implemented)"))
            showMainMenu(terminal)
        }
        MENU_EXIT -> {
            terminal.println(magenta("Exiting the program. Goodbye!"))
        }
    }
}


private fun titleExample(): String {
    // Font: ANSI shadow
    val title = """
 ██████╗ ██████╗ ██╗     ██╗     ███████╗ ██████╗ ███████╗
██╔════╝██╔═══██╗██║     ██║     ██╔════╝██╔════╝ ██╔════╝
██║     ██║   ██║██║     ██║     █████╗  ██║  ███╗█████╗  
██║     ██║   ██║██║     ██║     ██╔══╝  ██║   ██║██╔══╝  
╚██████╗╚██████╔╝███████╗███████╗███████╗╚██████╔╝███████╗
 ╚═════╝ ╚═════╝ ╚══════╝╚══════╝╚══════╝ ╚═════╝ ╚══════╝
""".trim('\n')
    return buildString {
        for (line in title.lineSequence()) {
            val lerp = Oklab.interpolator {
                stop(SRGB("#e74856"))
                stop(SRGB("#9648e7"))
            }.sequence(line.length)
            line.asSequence().zip(lerp).forEach { (c, color) ->
                append(TextColors.color(color)(c.toString()))
            }
            append("\n")
        }
    }.replace(Regex("""[╔═╗║╚╝]""")) { shadowColor(it.value) }
}
