# Como abrir e configurar o LivroHub

## Estado atual

O projeto Android ja esta estruturado e configurado com Kotlin, Jetpack Compose, Room, Coroutines e java-diff-utils.

Tambem foram adicionados:

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.properties`

Ainda falta o arquivo binario:

- `gradle/wrapper/gradle-wrapper.jar`

Ele nao foi baixado nesta sessao porque o ambiente Windows retornou erro TLS/credenciais ao tentar acessar o GitHub.

## Caminho recomendado

1. Abra esta pasta no Android Studio:

   `<caminho-do-projeto>\papel-voc-um-a-desenvolvedor-a`

2. Aguarde o Android Studio sincronizar o projeto.

3. Se o Android Studio reclamar que o Gradle Wrapper esta incompleto, use uma destas opcoes:

   - Abra o terminal do Android Studio e rode:

     ```bat
     gradle wrapper --gradle-version 8.7
     ```

   - Ou use o menu do Android Studio para atualizar/gerar o Gradle Wrapper.

4. Garanta que o Android SDK esteja instalado com uma plataforma compativel com `compileSdk = 35`.

5. Rode o app no emulador ou aparelho fisico.

## Observacao sobre `local.properties`

O arquivo `local.properties` nao foi criado porque nao encontrei Android SDK instalado nos caminhos comuns desta maquina.
Quando o projeto for aberto no Android Studio, ele normalmente cria esse arquivo automaticamente com o caminho correto do SDK.
