# Tricky Java Obfuscator

🔒 **Java-обфускатор с функцией краша декомпилятора, динамическим преобразователем вызова и шифрованием строк.** Поможет вам защитить ваш код от декомпиляции и анализа.

## 🌟 Особенности
1. **Flow**: Использование мусорного кода для усложнения анализа.
2. **InvokeDynamic**: Скрытие вызовов методов через handle-методы.
3. **String Encryption**: Шифрование всех строк с помощью AES.
4. **Crasher**: Ломает декомпилятор и не позволяет просто посмотреть исходный код.

До обфускации: [скачать .jar](https://github.com/superjavacrawler/trickyjavaobfuscator/raw/refs/heads/main/examples/Before-Obf-helloworld.jar)
![изображение](https://github.com/user-attachments/assets/0e444940-7998-4bae-925e-663c0c89579c)

После обфускации: [скачать .jar](https://github.com/superjavacrawler/trickyjavaobfuscator/raw/refs/heads/main/examples/After-Obf-helloworld-out.jar)
![изображение](https://github.com/user-attachments/assets/fa2ee4cb-749f-43dd-9197-61e390382547)

Если все таки декомпилировать, после обфускации код выглядит следующим образом: [открыть картинку](https://github.com/superjavacrawler/trickyjavaobfuscator/blob/main/after-obfuscation.png)

✋ Для использования, введите путь к `.jar` или его имя, если он в папке с обфускатором:
```bash
root@myvds $ [Tricky Java Obfuscator] Enter .jar name or path:
root@myvds $ MyTestProgram.jar
root@myvds $ [Tricky Java Obfuscator] Loaded file: C:\Users\javacrawler\Desktop\MyTestProgram.jar
root@myvds $ [Tricky Java Obfuscator] Processing obfuscation...
root@myvds $ [Tricky Java Obfuscator]
root@myvds $ [Tricky Java Obfuscator] com/testprogram/withoneclass/Main | Class loaded!
root@myvds $ [Tricky Java Obfuscator] com/testprogram/withoneclass/Main | Generating handle methods...
root@myvds $ [Tricky Java Obfuscator] com/testprogram/withoneclass/Main | testMethod1 | Applying Flow obfuscation...
root@myvds $ [Tricky Java Obfuscator] com/testprogram/withoneclass/Main | testMethod1 | Applying Invoke Dynamic obfuscation...
root@myvds $ [Tricky Java Obfuscator] com/testprogram/withoneclass/Main | testMethod1 | Applying String Encryption obfuscation...
root@myvds $ [Tricky Java Obfuscator] com/testprogram/withoneclass/Main | testMethod1 | Done!
root@myvds $ [Tricky Java Obfuscator]
[Tricky Java Obfuscator] File saved to C:\Users\javacrawler\Desktop\MyTestProgram.jar
```

В случае возникновения проблем обращайтесь к [javacrawler.lol](https://javacrawler.lol/)
