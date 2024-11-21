package com.abdo21.mathparsecompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                App(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}


@Composable
fun App(modifier: Modifier = Modifier) {

    var text by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        ExpressionView(
            expression = text,
            modifier = Modifier.background(color = Color.Yellow)
        )

        Spacer(modifier = Modifier.height(20.dp))

        TextField(
            value = text,
            onValueChange = {
                text = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        )
    }
}

// this is the only api that should be used from the outside
@Composable
fun ExpressionView(
    expression: String,
    modifier: Modifier = Modifier,
) {
    val expParser = remember { ExpParser() }

    var rootNode = remember { mutableStateOf<AstNode?>(null) }

    LaunchedEffect(expression) {
        try {
            rootNode.value = expParser.parse(expression)
        } catch (ignored: Exception) {}
    }

    rootNode.value?.let {
        RootNode(
            rootNode = it,
            modifier = modifier
        )
    }
}


// Abstract Syntax Tree Node
sealed class AstNode

data class Number(val value: String) : AstNode()
data class Identifier(val value: String) : AstNode()
data class BinaryOperation(val left: AstNode, val operator: Char, val right: AstNode) : AstNode()
data class SqrtNode(val argument: AstNode) : AstNode()

class ExpParser {
    private var input: String = ""
    private var pos: Int = 0

    private fun currentChar(): Char {
        return if (pos < input.length) input[pos] else '\u0000'
    }

    private fun advance() { pos++ }

    private fun nextChar(): Char {
        advance()
        return currentChar()
    }

    fun parse(input: String): AstNode {
        pos = 0
        this.input = input
        return parseExpression()
    }

    private fun parseExpression(): AstNode {
        var node = parseTerm()

        var c = currentChar()

        val opSet = setOf('+', '-', '=')

        while (opSet.contains(c)) {
            advance()
            node = BinaryOperation(node, c, parseTerm())

            c = currentChar()
        }

        return node
    }

    private fun parseTerm(): AstNode {
        var node = parseFactor()

        var c = currentChar()

        val opSet = setOf('*', '/', '^')

        while (opSet.contains(c)) {
            advance()
            node = BinaryOperation(node, c, parseFactor())
            c = currentChar()
        }

        return node
    }

    private fun parseFactor(): AstNode {
        val c = currentChar()
        return when (c) {
            '(' -> {
                    advance() // consume '('
                    val node = parseExpression()

                    val c = currentChar()
                    require(c == ')')
                    advance()
                    node
                }
            in '0'..'9', '.' -> parseNumber()
            else -> parseIdentifier()
        }
    }

    private fun parseNumber(): AstNode {
        var c = currentChar()

        val number = buildString {
            while (c.isDigit()) {
                append(c)
                c = nextChar()
            }
        }.toString()

        return Number(number)
    }

    private fun parseIdentifier(): AstNode {
        var c = currentChar()
        val identifier = buildString {
            while (c.isLetterOrDigit()) {
                append(c)
                c = nextChar()
            }
        }.toString()

        return if ("sqrt" == identifier) {
            SqrtNode(parseFactor())
        } else {
            Identifier(identifier)
        }
    }
}

@Composable
fun RootNode(rootNode: AstNode, modifier: Modifier = Modifier) {
    when (rootNode) {
        is Number -> {
            Text(text = rootNode.value, modifier = modifier)
        }
        is BinaryOperation -> {
            when (rootNode.operator) {
                '+' -> Plus(
                    modifier = modifier,
                    left = { RootNode(rootNode.left, modifier) },
                    right = { RootNode(rootNode.right, modifier) }
                )
                '-' -> Minus(
                    modifier = modifier,
                    left = { RootNode(rootNode.left, modifier) },
                    right = { RootNode(rootNode.right, modifier) }
                )
                '*' -> Times(
                    modifier = modifier,
                    left = { RootNode(rootNode.left, modifier) },
                    right = { RootNode(rootNode.right, modifier) }
                )
                '/' -> Fraction(
                    modifier = modifier,
                    numerator = { RootNode(rootNode.left, modifier) },
                    denominator = { RootNode(rootNode.right, modifier) }
                )
                '=' -> Equal(
                    modifier = modifier,
                    left = { RootNode(rootNode.left, modifier) },
                    right = { RootNode(rootNode.right, modifier) }
                )
                '^' -> Power(
                    modifier = modifier,
                    base = { RootNode(rootNode.left, modifier) },
                    exponent = { RootNode(rootNode.right, modifier) }
                )
            }
        }
        is Identifier -> {
            Text(text = rootNode.value, modifier = modifier)
        }
        is SqrtNode -> {
            SquareRoot(
                modifier = modifier,
                content = { RootNode(rootNode.argument, modifier) }
            )
        }
    }
}

@Composable
fun Fraction(
    modifier: Modifier = Modifier,
    fractionLineThickness: Dp = 1.dp,
    fractionColor: Color = Color.Black,
    numerator: @Composable () -> Unit,
    denominator: @Composable () -> Unit
) {
    var numeratorWidth by remember { mutableIntStateOf(0) }
    var denominatorWidth by remember { mutableIntStateOf(0) }

    val fractionWidth = remember(numeratorWidth, denominatorWidth) {
        max(numeratorWidth, denominatorWidth)
    }

    val padding =  8.dp

    // Measure the widest text (numerator or denominator)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        // Numerator
        Box(modifier = Modifier.onSizeChanged { numeratorWidth = it.width }) {
            numerator()
        }

        // Fraction line
        HorizontalDivider(
            modifier = Modifier
                .width(padding + with(LocalDensity.current) { fractionWidth.toDp() }),
            color = fractionColor,
            thickness = fractionLineThickness
        )

        // Denominator
        Box(modifier = Modifier.onSizeChanged { denominatorWidth = it.width }) {
            denominator()
        }
    }
}


@Composable
private fun BinaryOperation(
    op: String,
    modifier: Modifier = Modifier,
    withSpace: Boolean = true,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        left()
        Text(text = if (withSpace) " $op " else op)
        right()
    }
}

@Composable
fun Equal(
    modifier: Modifier = Modifier,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) = BinaryOperation(
    modifier = modifier,
    op = "=",
    left = left,
    right = right
)

@Composable
fun Minus(
    modifier: Modifier = Modifier,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) = BinaryOperation(
    modifier = modifier,
    op = "-",
    left = left,
    right = right
)

@Composable
fun Plus(
    modifier: Modifier = Modifier,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) = BinaryOperation(
    modifier = modifier,
    op = "+",
    left = left,
    right = right
)

@Composable
fun Times(
    modifier: Modifier = Modifier,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) = BinaryOperation(
    modifier = modifier,
    op = "x",
    left = left,
    right = right
)
//x1=(b+-sqrt(b^2-4*a*c))/(2*a)
//y = a*
@Composable
fun Power(
    modifier: Modifier = Modifier,
    base: @Composable () -> Unit,
    exponent: @Composable () -> Unit
) {
    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .alignBy { 0 }
        ) {
            base()
        }
        Box(
            modifier = Modifier
                .alignBy { 0 }
        ) {
            val textStyle = LocalTextStyle.current
            CompositionLocalProvider(LocalTextStyle provides TextStyle(fontSize = textStyle.fontSize/2)) {
                exponent()
            }
        }
    }
}

@Composable
fun SquareRoot(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    content: @Composable () -> Unit
) {
    var contentWidth by remember { mutableIntStateOf(0) }
    var contentHeight by remember { mutableIntStateOf(0) }

    val paddingLeft = remember(contentHeight) { contentHeight/6f }

    val paddingTop =  2.dp
    val paddingTopPx = with(LocalDensity.current) { paddingTop.toPx() }

    val strokeWidth = with(LocalDensity.current) { 1.dp.toPx() }


    Box(
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
        ) {
            val height = size.height
            val width = size.width

            val path = Path().apply {
                moveTo(0f, 0.7f*height)
                lineTo(paddingLeft/2, height)
                lineTo(paddingLeft, paddingTopPx/2)
                lineTo(width, paddingTopPx/2)
            }

            drawPath(
                color = color,
                path = path,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                ),
            )
        }

        Row {
            Spacer(modifier = Modifier.width(with(LocalDensity.current) { paddingLeft.toDp() }))
            Column {
                Spacer(modifier = Modifier.height(paddingTop))
                Box(
                    modifier = Modifier
                        .onSizeChanged {
                            contentWidth = it.width
                            contentHeight = it.height
                        }
                ) {
                    content()
                }
            }
        }
    }
}