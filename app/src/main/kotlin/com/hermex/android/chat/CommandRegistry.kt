package com.hermex.android.chat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/** Actions executed when a slash command is sent (distinct from the suggestion for display). */
sealed class CommandAction {
    /** Direct steering directive: `/steer <instruction>`. Intercepts or directs model response. */
    data class Steer(val instruction: String) : CommandAction()

    /** Switch or toggle Eva duality mode: `/mode <sakura|yandere|toggle>`. */
    data class Mode(val target: String) : CommandAction()

    /** Inspect PC host & hardware telemetry: `/status` or `/pc`. */
    data object Status : CommandAction()

    /** Reveal Eva's current autonomous inner thought: `/thought` or `/diary`. */
    data object Thought : CommandAction()

    /** View SATANA_ULTRA67 sprint status and habit checklist: `/looksmax` or `/satana`. */
    data object Looksmax : CommandAction()

    /** Compact conversation context to save tokens: `/compact` or `/compress`. */
    data object Compact : CommandAction()

    /** Clear conversation history: `/clear` or `/reset`. */
    data object Clear : CommandAction()

    /** Show help and command list: `/help`. */
    data object Help : CommandAction()

    /** Continue the last response -- sends prompt to server. */
    data object Continue : CommandAction() { val prompt = "Continue" }

    /** Summarize the conversation -- sends prompt to server. */
    data object Summarize : CommandAction() { val prompt = "Summarize this conversation" }

    /** Edit the last message. */
    data object Edit : CommandAction()

    /** Search past sessions. */
    data object Search : CommandAction()

    /** Stop current streaming generation or running processes: `/stop`. */
    data object Stop : CommandAction()

    /** Retry / regenerate last assistant reply: `/retry`. */
    data object Retry : CommandAction()

    /** Undo last conversation turn: `/undo`. */
    data object Undo : CommandAction()

    /** Start a brand new session: `/new`. */
    data object NewSession : CommandAction()

    /** Switch or inspect active model: `/model [name]`. */
    data class Model(val model: String) : CommandAction()

    /** Switch or inspect active workspace: `/workspace [path]`. */
    data class Workspace(val path: String) : CommandAction()

    /** Open workspace terminal: `/terminal`. */
    data object Terminal : CommandAction()

    /** Show token usage and quota stats: `/usage`. */
    data object Usage : CommandAction()

    /** Change app theme/skin: `/theme [name]`. */
    data class Theme(val theme: String) : CommandAction()

    /** Set personality profile: `/personality [name]`. */
    data class Personality(val name: String) : CommandAction()

    /** List installed skills: `/skills [query]`. */
    data class Skills(val query: String) : CommandAction()

    /** Force model to consult skill: `/use <skill>`. */
    data class UseSkill(val skill: String) : CommandAction()

    /** Set or manage goal: `/goal [status|pause|resume|clear|text]`. */
    data class Goal(val arg: String) : CommandAction()

    /** Add extra criteria to active goal: `/subgoal <text>`. */
    data class Subgoal(val text: String) : CommandAction()

    /** Queue a prompt for next turn without interrupting: `/queue <prompt>`. */
    data class Queue(val prompt: String) : CommandAction()

    /** Interrupt current turn and steer: `/interrupt <msg>`. */
    data class Interrupt(val message: String) : CommandAction()

    /** Rename current session title: `/title [title]`. */
    data class Title(val title: String) : CommandAction()

    /** Ask side question without interrupting context: `/btw <question>`. */
    data class Btw(val question: String) : CommandAction()

    /** Run prompt in background session: `/background <prompt>` or `/bg`. */
    data class Background(val prompt: String) : CommandAction()

    /** Toggle voice input or mode: `/voice [on|off]`. */
    data class Voice(val arg: String) : CommandAction()

    /** Configure reasoning display and effort: `/reasoning [level]`. */
    data class Reasoning(val level: String) : CommandAction()

    /** Toggle YOLO mode (skip confirmations): `/yolo`. */
    data object Yolo : CommandAction()

    /** Branch conversation from current point: `/branch [name]`. */
    data class Branch(val name: String) : CommandAction()

    /** Manage Petdex mascot: `/pet [toggle|list|<slug>]`. */
    data class Pet(val arg: String) : CommandAction()

    /** Reload environment and configuration: `/reload`. */
    data object Reload : CommandAction()

    /** Reconnect MCP servers from config: `/reload-mcp`. */
    data object ReloadMcp : CommandAction()

    /** Rescan installed skills directory: `/reload-skills`. */
    data object ReloadSkills : CommandAction()

    /** View Nous credits balance: `/credits` or `/topup`. */
    data object Credits : CommandAction()

    /** Review memory and lessons: `/memory`. */
    data object Memory : CommandAction()

    /** View usage insights and analytics: `/insights`. */
    data object Insights : CommandAction()

    /** Inspect git diff in workspace: `/diff`. */
    data object Diff : CommandAction()

    /** Generate markdown plan: `/plan <task>`. */
    data class Plan(val task: String) : CommandAction()

    /** Run prompt via Mixture of Agents: `/moa <prompt>`. */
    data class Moa(val prompt: String) : CommandAction()

    /** Detailed context window gauge and stats: `/context` or `/ctx`. */
    data object ContextView : CommandAction()

    /** Show user permissions: `/whoami`. */
    data object WhoAmI : CommandAction()

    /** Show active profile info: `/profile`. */
    data object Profile : CommandAction()

    /** List and resume past sessions: `/sessions`. */
    data object Sessions : CommandAction()

    /** Show current agent config: `/config`. */
    data object Config : CommandAction()

    /** Manage tools: `/tools [list|enable|disable]`. */
    data class Tools(val arg: String) : CommandAction()

    /** Manage scheduled cron tasks: `/cron [subcommand]`. */
    data class Cron(val arg: String) : CommandAction()

    /** Copy last assistant response: `/copy`. */
    data object Copy : CommandAction()

    /** Show Hermes version: `/version`. */
    data object Version : CommandAction()
}

data class CommandSuggestion(
    val command: String,
    val description: String,
    val icon: ImageVector,
    val action: CommandAction? = null,
)

object CommandRegistry {
    val commands: List<CommandSuggestion> = listOf(
        // Core Hermex & Eva
        CommandSuggestion("/mode", "Переключить режим Евы: /mode <sakura|yandere>", Icons.Filled.SwapHoriz),
        CommandSuggestion("/steer", "Направить ход рассуждений: /steer <указание>", Icons.Filled.Navigation),
        CommandSuggestion("/status", "Телеметрия ПК, SSD Apacer и хоста", Icons.Filled.Computer, CommandAction.Status),
        CommandSuggestion("/thought", "Тайная мысль Евы из дневника", Icons.Filled.Psychology, CommandAction.Thought),
        CommandSuggestion("/looksmax", "Статус спринта SATANA 06.10", Icons.Filled.FitnessCenter, CommandAction.Looksmax),
        CommandSuggestion("/compact", "Сжать контекст для экономии токенов", Icons.Filled.Summarize, CommandAction.Compact),
        CommandSuggestion("/clear", "Очистить текущий диалог", Icons.Filled.DeleteSweep, CommandAction.Clear),
        CommandSuggestion("/continue", "Продолжить генерацию ответа", Icons.Filled.PlayArrow, CommandAction.Continue),
        CommandSuggestion("/summarize", "Сделать саммари диалога", Icons.Filled.Summarize, CommandAction.Summarize),
        CommandSuggestion("/edit", "Редактировать последнее сообщение", Icons.Filled.Edit, CommandAction.Edit),
        CommandSuggestion("/search", "Поиск по сохранённым сессиям", Icons.Filled.Search, CommandAction.Search),
        CommandSuggestion("/help", "Справка по всем командам", Icons.AutoMirrored.Filled.Help, CommandAction.Help),

        // Desktop Hermes parity commands
        CommandSuggestion("/stop", "Прервать генерацию / фоновые процессы", Icons.Filled.Stop, CommandAction.Stop),
        CommandSuggestion("/retry", "Перегенерировать последний ответ модели", Icons.Filled.Refresh, CommandAction.Retry),
        CommandSuggestion("/undo", "Отменить последний ход диалога", Icons.AutoMirrored.Filled.Undo, CommandAction.Undo),
        CommandSuggestion("/new", "Начать новую чистую сессию диалога", Icons.Filled.Add, CommandAction.NewSession),
        CommandSuggestion("/model", "Сменить модель: /model [name]", Icons.Filled.Psychology),
        CommandSuggestion("/workspace", "Сменить рабочую папку: /workspace [name]", Icons.Filled.Folder),
        CommandSuggestion("/terminal", "Открыть терминал рабочего пространства", Icons.Filled.Terminal, CommandAction.Terminal),
        CommandSuggestion("/usage", "Статистика использования токенов и квот", Icons.Filled.DataUsage, CommandAction.Usage),
        CommandSuggestion("/theme", "Сменить тему/скин: /theme [dark|light|system]", Icons.Filled.Palette),
        CommandSuggestion("/personality", "Выбрать пресет личности: /personality [name]", Icons.Filled.Face),
        CommandSuggestion("/skills", "Каталог установленных навыков агента", Icons.Filled.Extension),
        CommandSuggestion("/use", "Принудительно использовать навык: /use <skill>", Icons.Filled.Extension),
        CommandSuggestion("/goal", "Управление целью: /goal [status|pause|resume|clear|text]", Icons.Filled.Flag),
        CommandSuggestion("/subgoal", "Добавить критерий к цели: /subgoal <text>", Icons.Filled.FormatListBulleted),
        CommandSuggestion("/queue", "Поставить в очередь следующего шага: /queue <текст>", Icons.AutoMirrored.Filled.ArrowForward),
        CommandSuggestion("/interrupt", "Прервать ход и передать указание: /interrupt <текст>", Icons.Filled.Warning),
        CommandSuggestion("/title", "Переименовать сессию: /title [название]", Icons.Filled.Edit),
        CommandSuggestion("/btw", "Задать боковой вопрос без перебивания: /btw <вопрос>", Icons.Filled.QuestionAnswer),
        CommandSuggestion("/bg", "Запустить задачу в фоне: /bg <промпт>", Icons.Filled.PlayArrow),
        CommandSuggestion("/voice", "Переключить голосовой режим: /voice [on|off]", Icons.Filled.Mic),
        CommandSuggestion("/reasoning", "Уровень рассуждений: /reasoning [show|hide|low|high]", Icons.Filled.Lightbulb),
        CommandSuggestion("/yolo", "Режим YOLO (выполнение опасных команд без подтверждений)", Icons.Filled.Warning, CommandAction.Yolo),
        CommandSuggestion("/branch", "Создать ветку сессии: /branch [название]", Icons.Filled.ForkRight),
        CommandSuggestion("/pet", "Питомец-маскот: /pet [toggle|list|<slug>]", Icons.Filled.Pets),
        CommandSuggestion("/reload", "Перезагрузить переменные .env и конфиг", Icons.Filled.Refresh, CommandAction.Reload),
        CommandSuggestion("/reload-mcp", "Переподключить серверы MCP", Icons.Filled.Refresh, CommandAction.ReloadMcp),
        CommandSuggestion("/reload-skills", "Пересканировать установленные навыки", Icons.Filled.Refresh, CommandAction.ReloadSkills),
        CommandSuggestion("/credits", "Баланс кредитов Nous и подписка", Icons.Filled.AccountBalanceWallet, CommandAction.Credits),
        CommandSuggestion("/memory", "Просмотр памяти и долговременных уроков", Icons.Filled.Psychology, CommandAction.Memory),
        CommandSuggestion("/insights", "Аналитика использования и графики", Icons.Filled.Analytics, CommandAction.Insights),
        CommandSuggestion("/diff", "Git diff изменений в рабочей папке", Icons.Filled.Difference, CommandAction.Diff),
        CommandSuggestion("/plan", "Составить план без выполнения: /plan <задача>", Icons.Filled.Assignment),
        CommandSuggestion("/moa", "Запуск через Mixture of Agents: /moa <промпт>", Icons.Filled.Bolt),
        CommandSuggestion("/context", "Индикатор заполнения контекстного окна", Icons.Filled.DataUsage, CommandAction.ContextView),
        CommandSuggestion("/whoami", "Проверить права и уровень доступа", Icons.Filled.Person, CommandAction.WhoAmI),
        CommandSuggestion("/profile", "Информация об активном профиле", Icons.Filled.Person, CommandAction.Profile),
        CommandSuggestion("/sessions", "Список всех сессий диалогов", Icons.Filled.Folder, CommandAction.Sessions),
        CommandSuggestion("/config", "Текущие настройки конфигурации", Icons.Filled.Settings, CommandAction.Config),
        CommandSuggestion("/tools", "Управление инструментами: /tools [list|enable|disable]", Icons.Filled.Build),
        CommandSuggestion("/cron", "Управление расписанием и фоновыми задачами", Icons.Filled.Schedule),
        CommandSuggestion("/copy", "Скопировать последний ответ", Icons.Filled.ContentCopy, CommandAction.Copy),
        CommandSuggestion("/version", "Версия ядра Hermes Agent", Icons.Filled.Info, CommandAction.Version),
    )

    fun filter(query: String): List<CommandSuggestion> {
        if (!query.startsWith("/")) return emptyList()
        val search = query.substring(1).lowercase().trim()
        if (search.isEmpty()) return commands
        return commands.filter {
            it.command.substring(1).contains(search, ignoreCase = true) ||
                it.description.contains(search, ignoreCase = true)
        }
    }

    /** Returns the matching CommandAction if the text is a known slash command, null otherwise. */
    fun matchCommand(text: String): CommandAction? {
        val trimmed = text.trim()
        if (!trimmed.startsWith("/")) return null
        val cmdPart = trimmed.split(Regex("\\s+"), limit = 2)[0].lowercase()
        val arg = if (trimmed.contains(" ")) trimmed.substringAfter(" ").trim() else ""

        return when (cmdPart) {
            "/steer" -> CommandAction.Steer(arg)
            "/mode" -> CommandAction.Mode(arg)
            "/status", "/pc" -> CommandAction.Status
            "/thought", "/diary" -> CommandAction.Thought
            "/looksmax", "/satana" -> CommandAction.Looksmax
            "/compact", "/compress" -> CommandAction.Compact
            "/clear", "/reset" -> CommandAction.Clear
            "/help" -> CommandAction.Help
            "/continue" -> CommandAction.Continue
            "/summarize" -> CommandAction.Summarize
            "/edit" -> CommandAction.Edit
            "/search" -> CommandAction.Search
            "/stop" -> CommandAction.Stop
            "/retry" -> CommandAction.Retry
            "/undo" -> CommandAction.Undo
            "/new" -> CommandAction.NewSession
            "/model" -> CommandAction.Model(arg)
            "/workspace" -> CommandAction.Workspace(arg)
            "/terminal" -> CommandAction.Terminal
            "/usage" -> CommandAction.Usage
            "/theme" -> CommandAction.Theme(arg)
            "/personality" -> CommandAction.Personality(arg)
            "/skills" -> CommandAction.Skills(arg)
            "/use" -> CommandAction.UseSkill(arg)
            "/goal" -> CommandAction.Goal(arg)
            "/subgoal" -> CommandAction.Subgoal(arg)
            "/queue", "/q" -> CommandAction.Queue(arg)
            "/interrupt" -> CommandAction.Interrupt(arg)
            "/title" -> CommandAction.Title(arg)
            "/btw" -> CommandAction.Btw(arg)
            "/background", "/bg" -> CommandAction.Background(arg)
            "/voice" -> CommandAction.Voice(arg)
            "/reasoning" -> CommandAction.Reasoning(arg)
            "/yolo" -> CommandAction.Yolo
            "/branch" -> CommandAction.Branch(arg)
            "/pet" -> CommandAction.Pet(arg)
            "/reload" -> CommandAction.Reload
            "/reload-mcp" -> CommandAction.ReloadMcp
            "/reload-skills" -> CommandAction.ReloadSkills
            "/credits", "/topup" -> CommandAction.Credits
            "/memory" -> CommandAction.Memory
            "/insights" -> CommandAction.Insights
            "/diff" -> CommandAction.Diff
            "/plan" -> CommandAction.Plan(arg)
            "/moa" -> CommandAction.Moa(arg)
            "/context", "/ctx" -> CommandAction.ContextView
            "/whoami" -> CommandAction.WhoAmI
            "/profile" -> CommandAction.Profile
            "/sessions" -> CommandAction.Sessions
            "/config" -> CommandAction.Config
            "/tools" -> CommandAction.Tools(arg)
            "/cron" -> CommandAction.Cron(arg)
            "/copy" -> CommandAction.Copy
            "/version" -> CommandAction.Version
            else -> null
        }
    }
}
