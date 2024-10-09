# Tricky Java Obfuscator

🔒 **Java-обфускатор с функцией краша декомпилятора, динамическим преобразователем вызовов и шифрованием строк.** Поможет вам защитить ваш код от декомпиляции и анализа.

## 🌟 Особенности
1. **Flow**: Использование мусорного кода для усложнения анализа.
2. **InvokeDynamic**: Скрытие вызовов методов через handle-методы.
3. **String Encryption**: Шифрование всех строк с помощью AES.
4. **Crasher**: Ломает декомпилятор и не позволяет просто посмотреть исходный код.

> До обфускации: [Скачать .jar](https://github.com/superjavacrawler/trickyjavaobfuscator/raw/refs/heads/main/examples/Before-Obf-helloworld.jar)

> После обфускации: [Скачать .jar](https://github.com/superjavacrawler/trickyjavaobfuscator/raw/refs/heads/main/examples/After-Obf-helloworld-out.jar) | [Скриншот кода](https://raw.githubusercontent.com/superjavacrawler/trickyjavaobfuscator/refs/heads/main/examples/after-obfuscation.png)

✋ Для использования, введите путь к `.jar` или его имя, если он в папке с обфускатором:
```bash
root@myvds $ [Tricky Java Obfuscator] Enter .jar name or path:
root@myvds $ MyTestProgram.jar
root@myvds $ [Tricky Java Obfuscator] Loaded file: /root/obfuscator/MyTestProgram.jar
root@myvds $ [Tricky Java Obfuscator] Processing obfuscation...
root@myvds $ [Tricky Java Obfuscator]
root@myvds $ [Tricky Java Obfuscator] com/testprogram/withoneclass/Main | Class loaded!
root@myvds $ [Tricky Java Obfuscator] com/testprogram/withoneclass/Main | Generating handle methods...
root@myvds $ [Tricky Java Obfuscator] com/testprogram/withoneclass/Main | testMethod1 | Applying Flow obfuscation...
root@myvds $ [Tricky Java Obfuscator] com/testprogram/withoneclass/Main | testMethod1 | Applying Invoke Dynamic obfuscation...
root@myvds $ [Tricky Java Obfuscator] com/testprogram/withoneclass/Main | testMethod1 | Applying String Encryption obfuscation...
root@myvds $ [Tricky Java Obfuscator] com/testprogram/withoneclass/Main | testMethod1 | Done!
root@myvds $ [Tricky Java Obfuscator]
[Tricky Java Obfuscator] File saved to C/root/obfuscator/MyTestProgram.jar
```

❓ В случае возникновения проблем обращайтесь к [javacrawler.lol](https://javacrawler.lol/)
Обфускатор был собран из частей найденных в интернете и доведен до ума, а также возможности обфускации исполняемых приложений!
