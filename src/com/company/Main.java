package com.company;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import static com.company.RandSimpleNumber.randSimpleNumber;
import static java.math.BigInteger.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Main {
    //Факторы протокола:
    //- N - простое = 2*q+1, где q - простое
    static BigInteger q = valueOf(randSimpleNumber());
    static BigInteger N = q.multiply(TWO).add(ONE);
    //- g - генератор по mod N: для любого 0 < X < N существует единственный x такой, что g^x mod N = X
    static BigInteger g=valueOf(2);
    //- k - множитель (может быть хеш-функцией), для простоты и производительности примем его за константу = 3
    static final BigInteger k = valueOf(3);
    //Данные для клиента и сервера
    static BigInteger x;
    static String P;
    static String[] s = new String[2];
    static BigInteger[] v = new BigInteger[2];
    static String[] I = new String[2];
    static BigInteger[] A = new BigInteger[2];
    static BigInteger[] B = new BigInteger[2];
    static BigInteger[] S = new BigInteger[2];
    static BigInteger[] K = new BigInteger[2];

    public static void main(String[] args) throws NoSuchAlgorithmException {
        boolean control = true;
        //I. Регистрация.
        //  В процессе регистрации принимает участие два компонента - клиент и сервер.
        System.out.println("----------------------------------------------------------------");
        System.out.println("I. Регистрация: ");
        System.out.println("----------------------------------------------------------------");
        registration();
        //II. Аутентификация.
        //II.I Фаза 1.
        System.out.println("----------------------------------------------------------------");
        System.out.println("II. Аутентификация: ");
        System.out.println("        I Фаза 1: ");
        System.out.println("----------------------------------------------------------------");
        control=authPhaseOne();
        System.out.println("----------------------------------------------------------------");
        System.out.println("        II Фаза 2: ");
        System.out.println("----------------------------------------------------------------");
        if(control)
            authPhaseTwo();
        else System.out.println("Соединение прервано!");

    }
    //Регистрация
    private static void registration() throws NoSuchAlgorithmException {
        //На клиенте генерируется:
        //- s - случайная строка
        s[0] = randomStr();
        //- p - password
        P = "pass123";
        //- x = H(S, p)
        x = new BigInteger(hash(s[0], P));
        //- v = g^x mod N
        v[0] = g.modPow(x, N);

        //затем отсылается серверу :
        I[0]="user123";
        System.out.println("Отправляется на сервер: I = "+I[0]+"; s = "+ s[0]+"; v = "+v[0]);
        //     I - username
        I[1] = I[0];
        //     s - соль (случайная строка)
        s[1] = s[0];
        //     v - верификатор пароля
        v[1] = v[0];
    }
    //Аутентификация Фаза I
    private static boolean authPhaseOne() throws NoSuchAlgorithmException {
        BigInteger[] u = new BigInteger[2];
        // Клиент отправляет на сервер A и I:
        //    I - username
        I[0]="user123";
        //    генерирует a - случайное число
        BigInteger a = valueOf((int)(10+Math.random()*170));
        //    A = g^a mod N
        A[0] = g.modPow(a, N);
        System.out.println("Клиент отправляет на сервер A = "+A[0]+"; и I = "+I[0]);
        I[1]=I[0];
        A[1] = A[0];
        //Сервер должен убедиться, что A != 0
        if(A[1].compareTo(ZERO)!=0) System.out.println("A = "+A[1]+"; A != 0");
        else return false;
        // Затем сервер генерирует случайное число b
        BigInteger b = valueOf((int)(10+Math.random()*170));
        //    и вычисляет B = (k*v + g^b mod N) mod N
        B[1] = k.multiply(v[1]).add(g.modPow(b, N)).mod(N);
        //Затем сервер отсылает клиенту s и B
        System.out.println("Cервер отсылает клиенту s = "+s[1]+"; и B = "+B[1]);
        s[0]=s[1];
        B[0] = B[1];
        //Клиент проверяет, что B != 0
        if(B[0].compareTo(ZERO)!=0) System.out.println("B = "+B[0]+"; B != 0");
        else return false;
        //    Затем обе стороны вычисляют u = H(A, B)
        u[0] = new BigInteger(hash(A[0].toString(), B[1].toString()));
        u[1] = new BigInteger(hash(A[0].toString(), B[1].toString()));
        System.out.println("u у клиента = "+u[0]+"\nu у сервераа = "+u[1]);
        //Если u = 0, то соединение прерывается
        if(u[0].compareTo(ZERO)!=0 && u[1].compareTo(ZERO)!=0){
            System.out.println("u != 0");
        }else return false;
        // Клиент на основе введённого пароля p вычисляет общий ключ сессии(K):
        //    x = H(s, p)
        x = new BigInteger(hash(s[1], P));
        //    S = ( ( B - k * (g^x mod N) ) ^ (a + u * x) ) mod N
        S[0] = B[1].subtract(k.multiply(g.modPow(x, N))).modPow(a.add(u[0].multiply(x)), N);
        //    K = H(S)
        K[0] = new BigInteger(hash(S[0].toString()));
        System.out.println("Клиент получил ключ: "+K[0]);
        //    Сервер вычисляет общий ключ сессии:
        //    S = ( (A * (v^u mod N)) ^ b ) % N
        S[1] = A[0].multiply(v[1].modPow(u[1], N)).modPow(b, N);
        //    K = H(S)
        K[1] = new BigInteger(hash(S[1].toString()));
        System.out.println("Сервер вычислил ключ: "+K[1]);
        //    После этого сервер и клиент - оба имеют одинаковые К.
        if(K[0].equals(K[1])){
            System.out.println("сервер и клиент - оба имеют одинаковые К");
        }else return false;
        return true;
    }
    //Аутентификация Фаза II
    private static void authPhaseTwo() throws NoSuchAlgorithmException {
        BigInteger[] M = new BigInteger[2];
        BigInteger[] R = new BigInteger[2];
        //    Генерация подтверждения.
        //    Клиент: M = H( H(N) xor H(g), H(I), S, A, B, k )
        M[0] = new BigInteger(hash(
                new String(xor(hash(N.toString()), hash(g.toString()))),
                new String(hash(I[1])),
                S[0].toString() , A[0].toString(), B[0].toString(), k.toString()
        ));
        System.out.println("Клиент вычислил M: "+M[0]);
        //Сервер: вычисляет М и если оно равно М от клиента, то всё ок и клиенту отсылается R = H (A, M, K)
        //Клиент получает R только в том случае, если М вычисленный на сервере равен М от клиента.
        M[1] = new BigInteger(hash(
                new String(xor(hash(N.toString()), hash(g.toString()))),
                new String(hash(I[1])),
                S[1].toString() , A[1].toString(), B[1].toString(), k.toString()
        ));
        System.out.println("Клиент вычислил M: "+M[1]);
        if(M[0].equals(M[1]))
            System.out.print("М клиента и сервера равны, сервер отправляет клиенту R = ");
        R[1] = new BigInteger(hash(A[1].toString(), M[1].toString(), K[1].toString()));
        System.out.println(R[1]);
        //Если вычисленная клиентом R = R c сервера, то клиент и сервер те за кого себя выдают.
        R[0] = new BigInteger(hash(A[0].toString(), M[0].toString(), K[0].toString()));
        System.out.println("R вычисленная клиентом = "+R[0]);
        if(R[0].equals(R[1]))
            System.out.println("Вычисленная клиентом R = R c сервера, клиент и сервер те, за кого себя выдают");
    }
    //- s - случайная строка
    private static String randomStr() {

        byte[] array = new byte[8];
        new Random().nextBytes(array);
        return new String(array, UTF_8);
    }
    //- хеш функция H (SHA2-512, https://en.wikipedia.org/wiki/SHA-2) - один из компонентов, который влияет на трудоёмкость вычислений в протоколе
    private static byte[] hash(String... args) throws NoSuchAlgorithmException {
        String source = "";
        for (String string : args) {
            source = source + string;
        }
        return MessageDigest.getInstance("SHA-512").digest((source).getBytes());
    }
    //формула для XoR
    private static byte[] xor(byte[] a, byte[] b) {
        byte[] result = new byte[Math.min(a.length, b.length)];

        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (((int) a[i]) ^ ((int) b[i]));
        }

        return result;
    }
}
