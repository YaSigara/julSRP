package com.company;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import static java.math.BigInteger.*;

public class Main {
    //Данные для клиента и сервера
    public static void main(String[] args) throws NoSuchAlgorithmException {
        //Факторы протокола:
        BigInteger q = valueOf(SluchainoeProstoe.sluchainoeProstoe()); //- N - простое = 2*q+1, где q - простое
        BigInteger N = q.multiply(TWO).add(ONE);
        BigInteger g=valueOf(2);                 //- g - генератор по mod N: для любого 0 < X < N существует единственный x такой, что g^x mod N = X
        final BigInteger k = valueOf(3);        //- k - множитель (может быть хеш-функцией), для простоты и производительности примем его за константу = 3
        //Регистрация
        BigInteger v;
        String s = randomStr();//случайная строка
        String P = "parolmne45let";//пароль
        BigInteger x = new BigInteger(hash(s, P));// x = H(S, p)
        v = g.modPow(x, N);// верификатор паолья v = g^x mod N
        //затем отсылается серверу :
        String I="juliaRum";
        System.out.println("Отправляется на сервер: I = "+I+"; s = "+ s+"; v = "+v);
        //аутентификация
        autorisation(v,N,g,k,I,s,P,x);
    }
    private static void autorisation(BigInteger v, BigInteger N, BigInteger g, BigInteger k, String I, String s, String P, BigInteger x) throws NoSuchAlgorithmException {
        //faza1 вернет массив значений, необходимых в вычислениях для faza2
        BigInteger[] data=faza1(v,N,g,k,I,s,P,x);
        faza2(data,N,g,k,I);
    }
    private static void faza2(BigInteger[] data, BigInteger N, BigInteger g, BigInteger k, String I) throws NoSuchAlgorithmException {
        BigInteger S1=data[0], S2=data[1], K1=data[2],K2=data[3],A=data[4],B=data[5];
        BigInteger M1,M2,R1,R2;
        //    Генерация подтверждения.
        M1 = new BigInteger(hash(
                new String(xor(hash(N.toString()), hash(g.toString()))),
                new String(hash(I)),
                S1.toString() , A.toString(), B.toString(), k.toString()
        ));//    Клиент: M = H( H(N) xor H(g), H(I), S, A, B, k )
        //Сервер: вычисляет М и если оно равно М от клиента, то всё ок и клиенту отсылается R = H (A, M, K)
        M2 = new BigInteger(hash(
                new String(xor(hash(N.toString()), hash(g.toString()))),
                new String(hash(I)),
                S2.toString() , A.toString(), B.toString(), k.toString()
        ));
        if(M1.equals(M2))
            System.out.print("М клиента и сервера равны");
        R2 = new BigInteger(hash(A.toString(), M1.toString(), K2.toString()));
        System.out.println("R сервера: "+R2);
        R1 = new BigInteger(hash(A.toString(), M2.toString(), K1.toString()));
        System.out.println("R клиента: "+R1);
        if(R1.equals(R2)){
            System.out.println("Вычисленные R сервера и R клиента равны. Пользователь и клиент те, за кого себя выдают");
        }


    }
    private static BigInteger[] faza1(BigInteger v, BigInteger N, BigInteger g, BigInteger k, String i, String s, String P, BigInteger x) throws NoSuchAlgorithmException {
        BigInteger[] outdata=new BigInteger[9];
        BigInteger u1,u2, K1,K2,S,A,B;                                              // Клиент отправляет на сервер A и I:
        Random rand = new Random();
        BigInteger a = BigInteger.valueOf(2+rand.nextInt(250));                //    генерирует a - случайное число
        A = g.modPow(a, N);                                                         //    A = g^a mod N
        if(!A.equals(BigInteger.ZERO)) {                                //Сервер должен убедиться, что A != 0
            System.out.println(A+" != 0");
        }
        BigInteger b = BigInteger.valueOf(2+rand.nextInt(250));     // Cервер генерирует случайное число b
        B = k.multiply(v).add(g.modPow(b, N)).mod(N);                   //вычисляет B = (k*v + g^b mod N) mod N
                                                                        //Затем сервер отсылает клиенту s и B

        if(!B.equals(BigInteger.ZERO)) {                                        //Клиент проверяет, что B != 0
            System.out.println(B+" != 0");
        }
        u1 = new BigInteger(hash(A.toString(), B.toString()));                     // Затем обе стороны вычисляют u = H(A, B)
        u2 = new BigInteger(hash(A.toString(), B.toString()));
        if(!(u1.equals(BigInteger.ZERO) || u2.equals(BigInteger.ZERO))){              //Если u = 0, то соединение прерывается
            System.out.println(u1+" != 0");
        }
                                                                        // Клиент на основе введённого пароля p вычисляет общий ключ сессии(K):
        x = new BigInteger(hash(s,P));
        S = B.subtract(k.multiply(g.modPow(x, N))).modPow(a.add(u1.multiply(x)), N); //    S = ( ( B - k * (g^x mod N) ) ^ (a + u * x) ) mod N
        outdata[0]=S;
        K1 = new BigInteger(hash(S.toString()));                                     //    K = H(S)
        //    Сервер вычисляет общий ключ сессии:
        S = A.multiply(v.modPow(u1, N)).modPow(b, N);                             //    S = ( (A * (v^u mod N)) ^ b ) % N
        outdata[1]=S;
        K2 = new BigInteger(hash(S.toString()));
        System.out.println("K1: "+K1+";\n K2: "+K2);
                                                                        //    После этого сервер и клиент - оба имеют одинаковые К.
        if(K1.equals(K2)){
            System.out.println("Ключи совпадают");
        }
        //параметры передаются в faze2
        outdata[2]=K1;
        outdata[3]=K2;
        outdata[4]=A;
        outdata[5]=B;
        return outdata;
    }
    //- s - случайная строка
    private static String randomStr() {
        byte[] str= new byte[8];
        new Random().nextBytes(str);
        return new String(str);
    }
    //- хеш функция H (SHA2-512, https://en.wikipedia.org/wiki/SHA-2) - один из компонентов, который влияет на трудоёмкость вычислений в протоколе
    private static byte[] hash(String... args) throws NoSuchAlgorithmException {
        StringBuilder source = new StringBuilder();
        for (String string : args) {
            source.append(string);
        }
        return MessageDigest.getInstance("SHA-512").digest(source.toString().getBytes());
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
