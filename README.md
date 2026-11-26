# Jogo da Cobrinha

Projeto de estudo em Java que implementa o jogo da cobrinha com interface gráfica Swing. O jogador controla a cobrinha, coleta a comida para crescer e ganhar pontos, e perde ao bater na parede ou no próprio corpo. A interface possui tema escuro, tabuleiro texturizado, elementos com profundidade e animação suave de entrada. A cor da cobrinha pode ser alterada pelo seletor no cabeçalho.

## Requisitos e execução

- JDK 17 ou superior
- Não requer bibliotecas externas

Na raiz do projeto, compile e execute:

```powershell
javac -encoding UTF-8 -d out src/main/java/br/com/jogopoo/*.java
java -cp out br.com.jogopoo.Main
```

## Como jogar

- Clique no botão de início ou pressione **espaço** / **Enter** para começar ou reiniciar.
- Use as **setas** ou **W, A, S, D** para mover a cobrinha.
- Coma a comida para crescer e aumentar a pontuação.
- Use **TROCAR COR** no cabeçalho para escolher outra cor. A partida pausa enquanto o seletor está aberto.

## Organização do projeto

| Classe | Responsabilidade |
| --- | --- |
| [`Main.java`](src/main/java/br/com/jogopoo/Main.java) | Inicia a aplicação Swing na thread de eventos. |
| [`JogoCobrinha.java`](src/main/java/br/com/jogopoo/JogoCobrinha.java) | Cria a janela principal do jogo. |
| [`PainelJogo.java`](src/main/java/br/com/jogopoo/PainelJogo.java) | Desenha o tabuleiro e a interface, recebe os controles, atualiza a partida e verifica colisões. |
| [`ElementoJogo.java`](src/main/java/br/com/jogopoo/ElementoJogo.java) | Classe abstrata base dos objetos desenhados no tabuleiro. |
| [`Cobrinha.java`](src/main/java/br/com/jogopoo/Cobrinha.java) | Controla movimento, direção, corpo, crescimento, aparência e desenho da cobrinha. |
| [`Comida.java`](src/main/java/br/com/jogopoo/Comida.java) | Representa e desenha a comida no tabuleiro. |

## Conceitos de POO no código

### Abstração

Abstrair é representar as características e operações importantes de uma ideia sem especificar todos os detalhes de cada caso. No jogo, os elementos têm uma posição e podem ser desenhados, mas a forma de desenhar depende do tipo de elemento.

[`ElementoJogo.java`](src/main/java/br/com/jogopoo/ElementoJogo.java) expressa essa ideia com uma classe abstrata:

```java
public abstract class ElementoJogo {
    private int x;
    private int y;

    public abstract void desenhar(
            Graphics2D graficos, int tamanhoCelula, double progresso);
}
```

`ElementoJogo` não pode ser instanciada diretamente: ela define a estrutura comum — coordenadas e a obrigação de desenhar —, mas não sabe se deve desenhar um corpo segmentado ou uma comida redonda. Por isso, `desenhar(...)` não tem implementação nessa classe. As classes concretas completam essa parte.

**No jogo:** `PainelJogo` pode trabalhar com qualquer elemento do tabuleiro sem conhecer previamente os detalhes visuais dele. A abstração evita colocar em uma única classe regras e desenhos específicos de todos os objetos.

### Encapsulamento

Encapsular é proteger o estado interno de um objeto e oferecer operações controladas para consultá-lo ou modificá-lo. Assim, outras classes pedem que o objeto execute uma ação em vez de alterar seus dados diretamente.

Em `ElementoJogo`, `x` e `y` são privados. A posição é consultada por `getX()` e `getY()`, enquanto a alteração passa por `definirPosicao(...)`, um método não público criado para a classe base e suas subclasses, sem expor um setter público.

Em [`Cobrinha.java`](src/main/java/br/com/jogopoo/Cobrinha.java), o corpo e a direção também são privados. A direção só pode ser alterada pelo método `mudarDirecao(...)`, que impede uma inversão imediata:

```java
private final List<Point> corpo = new ArrayList<>();
private Direcao direcao = Direcao.DIREITA;

public void mudarDirecao(Direcao novaDirecao) {
    if (novaDirecao != direcao.oposta()) {
        direcao = novaDirecao;
    }
}
```

O painel controla a cobrinha por operações como `mover()`, `crescer()` e `definirCor()`. Ele não recebe a lista do corpo nem altera diretamente a direção. Isso ajuda a manter regras importantes — como o tamanho do corpo e a proibição de virar diretamente para trás — dentro da classe responsável por elas.

### Herança

Herança permite criar uma classe especializada a partir de outra. A classe filha recebe os membros acessíveis da classe base e pode acrescentar seus próprios dados e comportamentos.

[`Cobrinha.java`](src/main/java/br/com/jogopoo/Cobrinha.java) declara:

```java
public class Cobrinha extends ElementoJogo {
    public Cobrinha(int x, int y) {
        super(x, y);
        // Inicializa os segmentos do corpo.
    }
}
```

[`Comida.java`](src/main/java/br/com/jogopoo/Comida.java) também estende `ElementoJogo`. Portanto, ambos herdam a posição e os métodos `getX()` e `getY()`. Cada um adiciona o que lhe é próprio: a cobrinha controla corpo, direção e crescimento; a comida pode ser reposicionada. O uso de `super(x, y)` inicializa as coordenadas definidas pela classe base.

**Relação “é um”:** uma cobrinha é um elemento do jogo, assim como uma comida. Essa relação permite que ambos sejam usados onde o código espera um `ElementoJogo`.

### Polimorfismo

Polimorfismo significa que objetos de classes diferentes podem ser tratados pelo mesmo tipo base e responder de maneiras próprias à mesma operação. Em Java, a implementação executada é escolhida conforme o tipo real do objeto.

Em [`PainelJogo.java`](src/main/java/br/com/jogopoo/PainelJogo.java), os elementos são armazenados como `ElementoJogo` e desenhados por um único laço:

```java
for (ElementoJogo elemento : elementos) {
    elemento.desenhar(tabuleiro, TAMANHO_CELULA, progresso);
}
```

O laço não precisa testar se o item é uma cobrinha ou uma comida:

- `Cobrinha.desenhar(...)` desenha os segmentos, a cabeça e os olhos, interpolando o movimento.
- `Comida.desenhar(...)` desenha a comida como um círculo.

As duas classes sobrescrevem (`@Override`) o método abstrato de `ElementoJogo`. Quando o laço chama `desenhar(...)`, Java encaminha a chamada para a implementação correspondente ao objeto. Se um novo elemento for criado, ele poderá fornecer seu próprio desenho sem exigir um novo `if` ou `switch` nesse laço.

## Outros recursos de orientação a objetos

- `Cobrinha.Direcao` é um `enum` que limita as direções possíveis e impede uma inversão direta de sentido.
- O seletor de cor altera a aparência por meio de `Cobrinha.definirCor()`, sem expor os campos de cor.
- A lógica da partida fica em `PainelJogo`, enquanto os objetos do jogo cuidam dos próprios dados e da própria representação.
