# MedReminder — Registro da Tarefa

## OBJETIVO
App Android nativo (Kotlin + Compose + Room) de lembrete de remédios: cadastro, agendamento (horários fixos / intervalo), alarmes exatos com notificação de alta prioridade (Tomar / Adiar 10 min), tela Hoje com 1 toque, histórico 7-30 dias com exportação, 100% offline. Compilar APK via GitHub Actions e publicar Release.

## STACK
- Kotlin 1.9.24, Compose Compiler 1.5.14, AGP 8.5.2, Gradle 8.7
- Compose BOM 2024.10.00 (Compose 1.7.5 / Material3 1.3.0), material-icons-extended
- Room 2.6.1 (kapt), coroutines 1.8.1
- minSdk 26, targetSdk/compileSdk 34

## PASSOS
- [x] Estrutura do projeto + data layer (entities/DAOs/DB/repository)
- [x] Alarm layer (scheduler/receivers/notification)
- [x] UI (home/add/history/settings) + tema
- [x] Workflow de build no GitHub Actions
- [x] Diagnosticar erros de compilação do build
- [x] Reescrever data layer: Flow em vez de LiveData, imports Calendar/ScheduleType, parse JSON com org.json
- [x] Reescrever alarm layer: consts públicas, Context no lugar de Application() manual, return@launch, goAsync
- [x] Reescrever CommonComponents + 4 telas com APIs Compose 1.7/Material3 1.3 válidas (SuggestionChip, FilterChip, sem FilledButton/SegmentedButton/stickyHeader)
- [x] Corrigir MainActivity (getInstance(Context), tema por SharedPreferences, fillMaxSize) e Theme (LocalView)
- [ ] Commit + push
- [ ] Build verde no GitHub Actions
- [ ] APK publicado na Release + links entregues

## DECISÕES
- Tema claro/escuro/sistema via SharedPreferences `medreminder_prefs` + Activity.recreate() (sem appcompat)
- Formulário de cadastro em tela única rolável (mais simples para idosos que 3 passos)
- Repositório expõe Flow; telas usam collectAsState
- getInstance(Context) em MedRepository e AlarmScheduler (nada de instanciar Application à mão)

## AGORA
Commit e push; aguardar build no GitHub Actions e corrigir o que sobrar até verde.
