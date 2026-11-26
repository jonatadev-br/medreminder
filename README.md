# MedReminder 💊

Um aplicativo Android nativo, simples e confiável para lembretes de medicamentos. Desenvolvido em **Kotlin + Jetpack Compose + Room**, com foco em acessibilidade, funcionamento offline e alta confiabilidade nos alarmes.

## 📱 Funcionalidades

### Cadastro de Medicamentos
- **Nome**, **dosagem** (ex: 500mg, 1 comprimido, 10 gotas)
- **Observações** (ex: "tomar em jejum", "após refeição")
- **Cor e ícone** personalizados para identificação visual rápida

### Agendamento Flexível
- **Horários fixos diários** (ex: 08:00 e 20:00)
- **Intervalos recorrentes** (a cada 6h, 8h ou 12h)
- **Duração do tratamento**: uso contínuo ou por X dias

### Sistema de Notificação e Alarme Confiável
- ✅ `AlarmManager.setExactAndAllowWhileIdle` — funciona mesmo em Doze/standby
- ✅ Notificações de **alta prioridade** com som e vibração
- ✅ Aparece na **tela de bloqueio** (fullScreenIntent)
- ✅ Botões de ação: **"Tomei"** e **"Adiar 10 min"**
- ✅ **Reagendamento automático** após reinicialização (`BOOT_COMPLETED`)
- ✅ Permissão `SCHEDULE_EXACT_ALARM` (Android 12+) solicitada na primeira abertura

### Tela Principal (Hoje)
- Lista cronológica das doses do dia
- Status claros: **Pendente**, **Tomado**, **Atrasado**, **Adiado**
- Botão grande "Tomar" acessível com 1 toque
- Botão "Adiar 10 min" nas doses pendentes

### Histórico (30 dias)
- Registro completo: tomado, atrasado, pulado
- Filtros: Todos / Tomados / Atrasados / Pulados
- Agrupado por data com headers fixos
- **Exportar resumo** em texto simples (compartilhar via WhatsApp, Email, Drive)

### 100% Offline
- Banco de dados local **Room (SQLite)**
- Sem login, sem internet, sem cadastro
- Seus dados ficam no seu dispositivo

## 🎨 Design e Acessibilidade
- **Material 3** com tema Claro/Escuro automático
- **Alto contraste** (WCAG AA)
- **Fontes escaláveis** (sp) — pensado para idosos
- **Touch targets ≥ 48dp**
- **Ícone adaptativo** vetorial
- Textos 100% em **Português do Brasil**

## 🏗️ Arquitetura

```
MedReminder/
├── data/
│   ├── entity/          # Medication, Schedule, DoseLog
│   ├── dao/             # Room DAOs
│   ├── database/        # AppDatabase + Converters
│   └── repository/      # MedRepository (single source of truth)
├── alarm/
│   ├── AlarmScheduler   # Agenda/cancela/reagenda AlarmManager
│   ├── AlarmReceiver    # Recebe alarme → dispara notificação
│   ├── BootReceiver     # BOOT_COMPLETED → reagenda tudo
│   ├── NotificationActionReceiver # Actions "Tomei"/"Adiar"
│   └── NotificationHelper # Canais, sons, vibração, actions
├── ui/
│   ├── theme/           # MaterialTheme, cores, tipografia, formas
│   ├── home/            # HomeScreen (Hoje) + DoseItem
│   ├── addmed/          # AddMedicationScreen (wizard 3 passos)
│   ├── history/         # HistoryScreen + Export
│   ├── settings/        # SettingsScreen (tema, permissões, teste)
│   └── components/      # Componentes reutilizáveis
└── util/                # DateTimeUtils, PermissionUtils
```

## 🔧 Build e Instalação

### Build na Nuvem (GitHub Actions)
O APK é compilado automaticamente no GitHub Actions a cada push na `main`:
- **Workflow**: `.github/workflows/build.yml`
- **JDK 17** + **Gradle** + **AGP 8.5.2**
- APK debug disponível nas **Releases**

### Build Local (se tiver Android SDK)
```bash
./gradlew assembleDebug
# APK em: app/build/outputs/apk/debug/app-debug.apk
```

### Instalação no Celular
1. Baixe o `app-debug.apk` da [Release mais recente](https://github.com/jonataprezende-commits/medreminder/releases/latest)
2. No celular, abra o arquivo APK
3. Permita "Instalar apps desconhecidos" para o navegador/gestor de arquivos
4. Instale e abra o app

## ⚙️ Permissões Necessárias (Primeira Abertura)

| Permissão | Para que serve | Android |
|-----------|----------------|---------|
| **Notificações** | Exibir lembretes com som/vibração | 13+ |
| **Alarmes exatos** | `setExactAndAllowWhileIdle` confiável | 12+ |
| **Inicialização** | Reagendar alarmes ao ligar o celular | Todas |

> O app solicita cada permissão com explicação clara. Se negar, pode conceder depois em **Configurações > Apps > MedReminder > Permissões**.

## 📋 Auditoria e Correções (Resumo)

Antes do build final, foi realizada auditoria completa:

| Área | Verificações |
|------|--------------|
| **Alarmes** | `setExactAndAllowWhileIdle` + `SCHEDULE_EXACT_ALARM` request + cancel no delete |
| **Boot** | `BootReceiver` exportado, `BOOT_COMPLETED` + `MY_PACKAGE_REPLACED` + `QUICKBOOT_POWERON` |
| **Notificações** | Canal `HIGH`, actions via `PendingIntent.getBroadcast`, `setAutoCancel`, fullScreenIntent |
| **Timezone** | Cálculos em `ZoneId.systemDefault()` — imune a mudança de fuso |
| **Intervalos** | Próxima dose baseada no **último "Tomou"** (não horário teórico) — evita acumular doses |
| **Fim de tratamento** | `endDate` respeitado; alarmes pós-`endDate` não criados |
| **Room** | Migrações versionadas (v1), TypeConverters para enums/JSON, transações no repository |
| **Permissões** | `POST_NOTIFICATIONS` (API 33+) + `SCHEDULE_EXACT_ALARM` (API 31+) com rationale |
| **Exportação** | Texto simples legível: data, medicamento, dosagem, horário agendado, status, horário tomado |
| **UI** | Tema claro/escuro, strings PT-BR, ícone adaptativo, empty states, loading |

## 📄 Licença

Este projeto é open-source sob licença MIT. Sinta-se livre para usar, modificar e distribuir.

---

**Desenvolvido com ❤️ por [zyvo](https://github.com/zyvo9/ZYVO-AI) + [Jonatas](https://github.com/jonataprezende-commits)**

> **Nota**: Este é um app de lembrete pessoal. Não substitui orientação médica profissional. Sempre consulte seu médico ou farmacêutico sobre seus medicamentos.