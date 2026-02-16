package com.example.yonosbi.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.yonosbi.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// -------------------- DATA MODELS --------------------

data class CarouselImage(
    val id: Int,
    val imageRes: Int
)

data class CreditCardItem(
    val id: Int,
    val title: String,
    val description: String
)

// -------------------- HOME SCREEN --------------------

@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val carouselImages = listOf(
        CarouselImage(1, R.drawable.top_card_1),
        CarouselImage(2, R.drawable.top_card_2),
        CarouselImage(3, R.drawable.top_card_3),
    )

    val creditCardItems = listOf(
        CreditCardItem(1, "Apply New Card", ""),
        CreditCardItem(2, "Reward Point", ""),
        CreditCardItem(3, "Annual Charges", ""),
        CreditCardItem(4, "Limit Increase", ""),
        CreditCardItem(5, "Separate Merged Cards", ""),
        CreditCardItem(6, "Instant Loan", ""),
        CreditCardItem(7, "Card Activation", ""),
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF1166DD) // 🔵 Blue background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // ⚪ White Content Container
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp
                        )
                    )
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                // 🔹 Carousel
                ImageCarouselWithDots(
                    images = carouselImages,
                    autoScrollInterval = 3000L
                )

                // 🔹 List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(creditCardItems.size) { index ->
                        CreditCardListItem(
                            item = creditCardItems[index],
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

// -------------------- CAROUSEL --------------------

@Composable
fun ImageCarouselWithDots(
    images: List<CarouselImage>,
    autoScrollInterval: Long = 3000L,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        pageCount = { images.size }
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        while (true) {
            delay(autoScrollInterval)
            val nextPage = (pagerState.currentPage + 1) % images.size
            coroutineScope.launch {
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) { page ->
            Image(
                painter = painterResource(id = images[page].imageRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(images.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index)
                                Color(0xFF1166DD)
                            else
                                Color.LightGray
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// -------------------- LIST ITEM --------------------

@Composable
fun CreditCardListItem(
    item: CreditCardItem,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val cardHeight = screenWidth * 0.52f
    val buttonHeight = screenWidth * 0.12f
    val buttonWidth = screenWidth * 0.4f
    val buttonRadius = buttonHeight / 2

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.card_image),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = item.title,
            textAlign = TextAlign.Center,
            fontSize = (screenWidth.value * 0.05f).sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { navController.navigate("user_details_form") },
            modifier = Modifier
                .width(buttonWidth)
                .height(buttonHeight),
            shape = RoundedCornerShape(buttonRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1166DD)
            )
        ) {
            Text(
                text = "Click Here",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
