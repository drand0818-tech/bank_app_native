package com.sbi.yonosbi.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sbi.yonosbi.R
import kotlinx.coroutines.delay

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
        containerColor = Color(0xFF1166DD)
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(
                            topStart = 28.dp,
                            topEnd = 28.dp
                        )
                    )
            ) {

                Spacer(modifier = Modifier.height(20.dp))

                ImageCarouselWithDots(
                    images = carouselImages,
                    autoScrollInterval = 3000L
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 20.dp,
                        vertical = 8.dp
                    )
                ) {
                    items(creditCardItems) { item ->
                        CreditCardListItem(
                            item = item,
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
    modifier: Modifier = Modifier,
    autoScrollInterval: Long = 3000L
) {
    val pagerState = rememberPagerState(pageCount = { images.size })

    LaunchedEffect(Unit) {
        while (true) {
            delay(autoScrollInterval)
            val nextPage = (pagerState.currentPage + 1) % images.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.2f),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) { page ->
            Image(
                painter = painterResource(id = images[page].imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.8f),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.card_image),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = item.title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("user_details_form") },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(48.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1166DD)
            )
        ) {
            Text(
                text = "Click Here",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}