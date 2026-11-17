"""Sample catalogue for Gola Grocery.

Prices are illustrative sample prices in INR. Barcodes are synthetic EAN-13s
(890 = India prefix) with valid check digits, generated for demo only.
Structure: CATEGORIES = [ (code, name, name_hi, color, [ (sub_name, sub_name_hi, [products]) ]) ]
product = (name, brand, description, gst, [variants]); variant = (label, qty, unit, mrp, price)
"""

def v(label, qty, unit, mrp, price):
    return (label, qty, unit, mrp, price)

F = "Gola Fresh"

CATEGORIES = [
 ("FV", "Fruits & Vegetables", "फल और सब्ज़ियाँ", "#2E7D32", [
   ("Fresh Fruits", "ताज़े फल", [
     ("Shimla Apple", F, "Crisp, juicy apples from Himachal orchards. Hand-picked and quality checked.", 0,
        [v("1 kg", 1, "KG", 220, 179), v("500 g", 500, "G", 115, 95)]),
     ("Robusta Banana", F, "Naturally ripened Robusta bananas, rich in potassium.", 0,
        [v("6 pcs", 6, "PCS", 60, 49), v("12 pcs", 12, "PCS", 115, 92)]),
     ("Alphonso Mango", F, "Ratnagiri Alphonso mangoes with rich aroma and sweetness. Seasonal.", 0,
        [v("1 kg", 1, "KG", 499, 429)]),
     ("Nagpur Orange", F, "Juicy, tangy Nagpur oranges packed with Vitamin C.", 0,
        [v("1 kg", 1, "KG", 140, 119)]),
     ("Green Seedless Grapes", F, "Sweet seedless grapes from Nashik vineyards.", 0,
        [v("500 g", 500, "G", 99, 79)]),
     ("Pomegranate", F, "Ruby-red arils, sweet and juicy.", 0, [v("4 pcs", 4, "PCS", 199, 159)]),
   ]),
   ("Fresh Vegetables", "ताज़ी सब्ज़ियाँ", [
     ("Potato", F, "Farm-fresh potatoes, ideal for sabzi, fries and parathas.", 0,
        [v("1 kg", 1, "KG", 45, 35), v("2 kg", 2, "KG", 88, 68)]),
     ("Onion", F, "Red Nashik onions, firm and pungent.", 0,
        [v("1 kg", 1, "KG", 55, 42), v("2 kg", 2, "KG", 108, 82)]),
     ("Tomato Hybrid", F, "Firm, red hybrid tomatoes for curries and salads.", 0,
        [v("1 kg", 1, "KG", 50, 39), v("500 g", 500, "G", 26, 21)]),
     ("Green Capsicum", F, "Crunchy green bell peppers.", 0, [v("500 g", 500, "G", 60, 48)]),
     ("Cauliflower", F, "Fresh white cauliflower head.", 0, [v("1 pc (approx. 600 g)", 1, "PCS", 45, 36)]),
     ("Bhindi (Lady Finger)", F, "Tender lady fingers, freshly harvested.", 0, [v("500 g", 500, "G", 45, 36)]),
     ("Green Chilli", F, "Spicy green chillies.", 0, [v("100 g", 100, "G", 15, 11)]),
     ("Ginger", F, "Fresh, aromatic ginger root.", 0, [v("250 g", 250, "G", 40, 32)]),
   ]),
   ("Leafy Vegetables", "पत्तेदार सब्ज़ियाँ", [
     ("Palak (Spinach)", F, "Fresh green spinach bunch, cleaned and ready to cook.", 0, [v("250 g", 250, "G", 30, 22)]),
     ("Coriander Leaves", F, "Fragrant fresh dhaniya bunch.", 0, [v("100 g", 100, "G", 20, 14)]),
     ("Methi (Fenugreek)", F, "Fresh methi leaves for parathas and sabzi.", 0, [v("250 g", 250, "G", 30, 24)]),
     ("Mint Leaves", F, "Cool pudina bunch for chutneys and drinks.", 0, [v("100 g", 100, "G", 20, 15)]),
   ]),
   ("Seasonal Produce", "मौसमी उत्पाद", [
     ("Sweet Corn", F, "Golden sweet corn cobs.", 0, [v("2 pcs", 2, "PCS", 50, 40)]),
     ("Green Peas (Matar)", F, "Fresh winter green peas in pods.", 0, [v("500 g", 500, "G", 60, 45)]),
     ("Carrot (Gajar)", F, "Red Delhi carrots, perfect for gajar halwa.", 0, [v("500 g", 500, "G", 40, 32)]),
   ]),
 ]),
 ("DB", "Dairy & Bakery", "डेयरी और बेकरी", "#1565C0", [
   ("Milk", "दूध", [
     ("Amul Taaza Toned Milk", "Amul", "Pasteurised toned milk, 3% fat.", 0,
        [v("500 ml", 500, "ML", 28, 28), v("1 L", 1, "L", 56, 56)]),
     ("Mother Dairy Full Cream Milk", "Mother Dairy", "Rich full cream milk, 6% fat.", 0,
        [v("500 ml", 500, "ML", 34, 34), v("1 L", 1, "L", 68, 68)]),
   ]),
   ("Curd & Paneer", "दही और पनीर", [
     ("Amul Masti Dahi", "Amul", "Thick and creamy curd made from pasteurised milk.", 5, [v("400 g", 400, "G", 35, 35)]),
     ("Mother Dairy Fresh Paneer", "Mother Dairy", "Soft, fresh malai paneer.", 5, [v("200 g", 200, "G", 95, 90)]),
   ]),
   ("Butter, Cheese & Ghee", "मक्खन, चीज़ और घी", [
     ("Amul Butter Pasteurised", "Amul", "Utterly butterly delicious salted butter.", 12,
        [v("100 g", 100, "G", 58, 58), v("500 g", 500, "G", 285, 275)]),
     ("Amul Cheese Slices", "Amul", "Processed cheese slices, 10 slices.", 12, [v("200 g", 200, "G", 150, 142)]),
     ("Amul Pure Ghee", "Amul", "Pure cow ghee with rich aroma.", 12, [v("1 L", 1, "L", 650, 615)]),
   ]),
   ("Eggs", "अंडे", [
     ("Farm Fresh White Eggs", F, "Protein-rich farm eggs.", 0, [v("6 pcs", 6, "PCS", 54, 48), v("12 pcs", 12, "PCS", 105, 92)]),
   ]),
   ("Breads & Bakery", "ब्रेड और बेकरी", [
     ("Britannia Whole Wheat Bread", "Britannia", "100% whole wheat bread, high in fibre.", 0, [v("450 g", 450, "G", 55, 52)]),
     ("Harvest Gold White Bread", "Harvest Gold", "Soft sandwich bread.", 0, [v("400 g", 400, "G", 45, 45)]),
     ("Britannia Gobbles Fruit Cake", "Britannia", "Soft fruit cake loaded with tutti frutti.", 18, [v("250 g", 250, "G", 90, 85)]),
     ("Pav Buns", "Harvest Gold", "Soft pav for pav bhaji and vada pav.", 0, [v("6 pcs", 6, "PCS", 35, 32)]),
   ]),
 ]),
 ("AR", "Atta, Rice & Dal", "आटा, चावल और दाल", "#8D6E63", [
   ("Atta & Flours", "आटा", [
     ("Aashirvaad Shudh Chakki Atta", "Aashirvaad", "Whole wheat atta, 0% maida, soft rotis.", 0,
        [v("5 kg", 5, "KG", 299, 265), v("10 kg", 10, "KG", 575, 509)]),
     ("Aashirvaad Multigrain Atta", "Aashirvaad", "Atta with 6 grains: wheat, soya, chana, oat, maize and psyllium husk.", 0,
        [v("5 kg", 5, "KG", 365, 329)]),
     ("Fortune Sooji (Rava)", "Fortune", "Fine semolina for upma and halwa.", 0, [v("500 g", 500, "G", 40, 36)]),
   ]),
   ("Rice & Poha", "चावल और पोहा", [
     ("India Gate Basmati Rice Classic", "India Gate", "Aged long-grain basmati, aromatic and fluffy.", 5,
        [v("1 kg", 1, "KG", 245, 215), v("5 kg", 5, "KG", 1150, 999)]),
     ("Daawat Rozana Basmati Rice", "Daawat", "Everyday basmati rice.", 5, [v("5 kg", 5, "KG", 499, 435)]),
     ("Sona Masoori Rice", "Gola Select", "Light, aromatic regular rice for daily meals.", 0, [v("5 kg", 5, "KG", 399, 345)]),
     ("Thick Poha", "Gola Select", "Thick flattened rice for poha.", 0, [v("500 g", 500, "G", 45, 38)]),
   ]),
   ("Dals & Pulses", "दालें", [
     ("Tata Sampann Moong Dal", "Tata Sampann", "Unpolished yellow moong dal.", 0, [v("1 kg", 1, "KG", 185, 159)]),
     ("Tata Sampann Masoor Dal", "Tata Sampann", "Unpolished masoor dal, rich in protein.", 0, [v("1 kg", 1, "KG", 145, 125)]),
     ("Tata Sampann Chana Dal", "Tata Sampann", "Unpolished chana dal.", 0, [v("1 kg", 1, "KG", 130, 112)]),
     ("Rajma Chitra", "Gola Select", "Speckled kidney beans for Punjabi rajma.", 0, [v("500 g", 500, "G", 99, 85)]),
     ("Kabuli Chana (Chickpeas)", "Gola Select", "Large white chickpeas for chole.", 0, [v("1 kg", 1, "KG", 180, 155)]),
   ]),
 ]),
 ("OG", "Oil & Ghee", "तेल और घी", "#F9A825", [
   ("Edible Oils", "खाद्य तेल", [
     ("Fortune Kachi Ghani Mustard Oil", "Fortune", "Pungent cold-pressed mustard oil.", 5,
        [v("1 L", 1, "L", 199, 175), v("5 L", 5, "L", 960, 845)]),
     ("Fortune Sunlite Sunflower Oil", "Fortune", "Refined sunflower oil, light and healthy.", 5, [v("1 L", 1, "L", 175, 155)]),
     ("Saffola Gold Refined Oil", "Saffola", "Blended rice bran and sunflower oil.", 5, [v("1 L", 1, "L", 230, 199)]),
     ("Dhara Groundnut Oil", "Dhara", "Filtered groundnut oil.", 5, [v("1 L", 1, "L", 235, 210)]),
     ("Parachute Coconut Oil", "Parachute", "100% pure coconut oil.", 5, [v("500 ml", 500, "ML", 220, 205)]),
   ]),
   ("Ghee", "घी", [
     ("Patanjali Cow Ghee", "Patanjali", "Pure desi cow ghee.", 12, [v("1 L", 1, "L", 640, 599)]),
     ("Gowardhan Pure Cow Ghee", "Gowardhan", "Pure cow ghee in tin.", 12, [v("500 ml", 500, "ML", 345, 320)]),
   ]),
 ]),
 ("MS", "Masala & Spices", "मसाले", "#D84315", [
   ("Powdered Spices", "पिसे मसाले", [
     ("Tata Sampann Turmeric Powder", "Tata Sampann", "Haldi with natural oils intact.", 5, [v("200 g", 200, "G", 72, 65)]),
     ("MDH Deggi Mirch", "MDH", "Kashmiri red chilli blend for colour and mild heat.", 5, [v("100 g", 100, "G", 95, 88)]),
     ("Everest Coriander Powder", "Everest", "Dhaniya powder with fresh aroma.", 5, [v("200 g", 200, "G", 70, 64)]),
     ("MDH Garam Masala", "MDH", "Classic blend of whole spices.", 5, [v("100 g", 100, "G", 88, 82)]),
   ]),
   ("Whole Spices", "साबुत मसाले", [
     ("Catch Jeera (Cumin Seeds)", "Catch", "Bold cumin seeds.", 5, [v("100 g", 100, "G", 80, 72)]),
     ("Rai (Mustard Seeds)", "Gola Select", "Small black mustard seeds for tadka.", 5, [v("100 g", 100, "G", 30, 25)]),
     ("Green Cardamom (Elaichi)", "Gola Select", "Bold green cardamom pods.", 5, [v("50 g", 50, "G", 220, 189)]),
     ("Whole Garam Masala Mix", "Gola Select", "Cloves, cinnamon, bay leaf, black pepper.", 5, [v("100 g", 100, "G", 120, 105)]),
   ]),
 ]),
 ("SN", "Snacks", "स्नैक्स", "#E65100", [
   ("Chips & Wafers", "चिप्स", [
     ("Lay's India's Magic Masala", "Lay's", "Potato chips with spicy Indian masala.", 12, [v("52 g", 52, "G", 20, 20), v("90 g", 90, "G", 50, 48)]),
     ("Uncle Chipps Spicy Treat", "Uncle Chipps", "Crispy spicy potato wafers.", 12, [v("55 g", 55, "G", 20, 20)]),
   ]),
   ("Namkeen & Bhujia", "नमकीन और भुजिया", [
     ("Haldiram's Aloo Bhujia", "Haldiram's", "Crunchy potato and besan sev.", 12, [v("400 g", 400, "G", 110, 99)]),
     ("Haldiram's Moong Dal", "Haldiram's", "Salted fried moong dal.", 12, [v("200 g", 200, "G", 55, 52)]),
     ("Bikaji Bikaneri Bhujia", "Bikaji", "Authentic Bikaneri bhujia.", 12, [v("400 g", 400, "G", 115, 105)]),
   ]),
   ("Biscuits & Cookies", "बिस्कुट और कुकीज़", [
     ("Parle-G Gold", "Parle", "India's favourite glucose biscuit.", 18, [v("1 kg", 1, "KG", 150, 140)]),
     ("Britannia Good Day Cashew", "Britannia", "Butter cookies with cashew.", 18, [v("200 g", 200, "G", 50, 47)]),
     ("Sunfeast Dark Fantasy Choco Fills", "Sunfeast", "Cookies with molten chocolate filling.", 18, [v("300 g", 300, "G", 180, 160)]),
   ]),
   ("Chocolates & Candies", "चॉकलेट और कैंडी", [
     ("Cadbury Dairy Milk Silk", "Cadbury", "Smooth, creamy milk chocolate.", 18, [v("60 g", 60, "G", 90, 90)]),
     ("Cadbury Celebrations Pack", "Cadbury", "Assorted chocolate gift pack.", 18, [v("177 g", 177, "G", 225, 210)]),
     ("Pulse Kachcha Aam Candy", "Pass Pass", "Tangy raw mango candy with masala centre.", 18, [v("Pack of 50", 50, "PCS", 50, 48)]),
   ]),
   ("Popcorn", "पॉपकॉर्न", [
     ("Act II Butter Lovers Popcorn", "Act II", "Microwave popcorn, rich butter flavour.", 12, [v("150 g", 150, "G", 70, 62)]),
   ]),
 ]),
 ("BV", "Beverages", "पेय पदार्थ", "#00838F", [
   ("Water & Soft Drinks", "पानी और शीतल पेय", [
     ("Bisleri Mineral Water", "Bisleri", "Packaged drinking water.", 18, [v("1 L", 1, "L", 20, 20), v("Pack of 12 x 1 L", 12, "L", 240, 220)]),
     ("Coca-Cola", "Coca-Cola", "Refreshing cola.", 28, [v("750 ml", 750, "ML", 40, 40), v("2.25 L", 2.25, "L", 99, 95)]),
     ("Thums Up", "Thums Up", "Strong, fizzy cola.", 28, [v("750 ml", 750, "ML", 40, 40)]),
   ]),
   ("Juices", "जूस", [
     ("Real Fruit Power Mixed Fruit", "Real", "Mixed fruit juice.", 12, [v("1 L", 1, "L", 130, 115)]),
     ("Tropicana 100% Orange Juice", "Tropicana", "No added sugar orange juice.", 12, [v("1 L", 1, "L", 160, 145)]),
   ]),
   ("Tea & Coffee", "चाय और कॉफ़ी", [
     ("Tata Tea Premium", "Tata Tea", "Strong blend of Assam tea.", 5, [v("500 g", 500, "G", 290, 265), v("1 kg", 1, "KG", 560, 510)]),
     ("Brooke Bond Red Label", "Red Label", "Natural care tea with 5 ayurvedic ingredients.", 5, [v("500 g", 500, "G", 280, 255)]),
     ("Nescafe Classic Instant Coffee", "Nescafe", "100% pure instant coffee.", 18, [v("100 g", 100, "G", 365, 340)]),
     ("Bru Instant Coffee", "Bru", "Coffee-chicory blend.", 18, [v("100 g", 100, "G", 250, 235)]),
     ("Lipton Honey Lemon Green Tea", "Lipton", "Green tea bags with honey and lemon.", 5, [v("25 bags", 25, "PCS", 175, 155)]),
   ]),
   ("Health Drinks", "हेल्थ ड्रिंक्स", [
     ("Bournvita Chocolate Health Drink", "Cadbury", "Malt-based chocolate health drink.", 18, [v("500 g", 500, "G", 245, 230)]),
     ("Horlicks Classic Malt", "Horlicks", "Nutritional malt drink.", 18, [v("500 g", 500, "G", 255, 240)]),
   ]),
 ]),
 ("PC", "Personal Care", "पर्सनल केयर", "#AD1457", [
   ("Bath & Body", "स्नान और शरीर", [
     ("Dove Cream Beauty Bathing Bar", "Dove", "Moisturising bathing bar with 1/4 moisturising cream.", 18, [v("Pack of 3 x 100 g", 3, "PCS", 199, 175)]),
     ("Lux Soft Glow Soap", "Lux", "Rose and Vitamin E soap.", 18, [v("Pack of 4 x 100 g", 4, "PCS", 160, 140)]),
     ("Dettol Original Soap", "Dettol", "Germ protection bathing soap.", 18, [v("Pack of 4 x 75 g", 4, "PCS", 180, 162)]),
   ]),
   ("Hair Care", "बालों की देखभाल", [
     ("Head & Shoulders Anti-Dandruff Shampoo", "Head & Shoulders", "Smooth and silky anti-dandruff shampoo.", 18, [v("340 ml", 340, "ML", 399, 349)]),
     ("Dove Hair Fall Rescue Conditioner", "Dove", "Reduces hair fall due to breakage.", 18, [v("180 ml", 180, "ML", 250, 225)]),
     ("Parachute Advansed Jasmine Hair Oil", "Parachute", "Coconut hair oil with jasmine.", 18, [v("300 ml", 300, "ML", 175, 160)]),
   ]),
   ("Oral Care", "मुँह की देखभाल", [
     ("Colgate Strong Teeth Toothpaste", "Colgate", "Calcium-boost toothpaste.", 18, [v("200 g", 200, "G", 120, 110)]),
     ("Colgate ZigZag Toothbrush", "Colgate", "Medium bristles, pack of 4.", 18, [v("Pack of 4", 4, "PCS", 120, 99)]),
   ]),
   ("Skin Care & Deodorants", "त्वचा देखभाल और डियो", [
     ("Nivea Soft Light Moisturising Cream", "Nivea", "Light cream with jojoba oil and Vitamin E.", 18, [v("100 ml", 100, "ML", 249, 225)]),
     ("Himalaya Purifying Neem Face Wash", "Himalaya", "Soap-free neem face wash.", 18, [v("150 ml", 150, "ML", 210, 189)]),
     ("Fogg Fresh Deodorant", "Fogg", "Long-lasting no-gas body spray.", 18, [v("150 ml", 150, "ML", 299, 249)]),
   ]),
   ("Feminine Hygiene", "स्त्री स्वच्छता", [
     ("Whisper Choice Ultra XL", "Whisper", "Sanitary pads with wings.", 12, [v("Pack of 20", 20, "PCS", 199, 185)]),
     ("Stayfree Secure XL", "Stayfree", "Cottony soft sanitary pads.", 12, [v("Pack of 18", 18, "PCS", 140, 130)]),
   ]),
 ]),
 ("HC", "Home Cleaning", "घर की सफ़ाई", "#6A1B9A", [
   ("Detergents", "डिटर्जेंट", [
     ("Surf Excel Easy Wash Detergent Powder", "Surf Excel", "Removes tough stains easily.", 18, [v("1 kg", 1, "KG", 140, 130), v("4 kg", 4, "KG", 545, 499)]),
     ("Ariel Matic Front Load Liquid", "Ariel", "Liquid detergent for front-load machines.", 18, [v("1 L", 1, "L", 315, 285)]),
   ]),
   ("Dishwash", "बर्तन धोना", [
     ("Vim Dishwash Liquid Gel Lemon", "Vim", "Power of 100 lemons.", 18, [v("750 ml", 750, "ML", 199, 175)]),
     ("Vim Dishwash Bar", "Vim", "Lemon dishwash bar.", 18, [v("Pack of 3 x 200 g", 3, "PCS", 60, 55)]),
   ]),
   ("Floor & Toilet Cleaners", "फ़र्श और टॉयलेट क्लीनर", [
     ("Lizol Disinfectant Floor Cleaner Citrus", "Lizol", "Kills 99.9% germs.", 18, [v("975 ml", 975, "ML", 229, 205)]),
     ("Harpic Power Plus Toilet Cleaner", "Harpic", "10x better cleaning.", 18, [v("1 L", 1, "L", 215, 189)]),
     ("Dettol Antiseptic Liquid", "Dettol", "Antiseptic disinfectant liquid.", 18, [v("550 ml", 550, "ML", 245, 229)]),
   ]),
   ("Tissues & Garbage Bags", "टिशू और कचरा बैग", [
     ("Origami So Soft Kitchen Towel", "Origami", "2-ply kitchen towels, 2 rolls.", 18, [v("2 rolls", 2, "PCS", 160, 139)]),
     ("Garbage Bags Medium (19x21)", "Gola Select", "Biodegradable garbage bags, 30 pcs per roll.", 18, [v("Pack of 3 rolls", 3, "PCS", 180, 145)]),
   ]),
 ]),
 ("BC", "Baby Care", "शिशु देखभाल", "#EC407A", [
   ("Diapers & Wipes", "डायपर और वाइप्स", [
     ("Pampers Pants Medium", "Pampers", "Diaper pants for 7-12 kg babies.", 12, [v("Pack of 56", 56, "PCS", 1299, 999)]),
     ("Himalaya Gentle Baby Wipes", "Himalaya", "Aloe vera and Indian kino tree wipes.", 12, [v("Pack of 72", 72, "PCS", 199, 175)]),
   ]),
   ("Baby Bath & Skin", "शिशु स्नान और त्वचा", [
     ("Johnson's Baby No More Tears Shampoo", "Johnson's", "Gentle, tear-free shampoo.", 18, [v("200 ml", 200, "ML", 199, 185)]),
     ("Himalaya Baby Lotion", "Himalaya", "With olive oil and almond oil.", 18, [v("200 ml", 200, "ML", 215, 199)]),
     ("Johnson's Baby Soap", "Johnson's", "Mild baby soap.", 18, [v("Pack of 3 x 75 g", 3, "PCS", 165, 150)]),
   ]),
   ("Baby Food", "शिशु आहार", [
     ("Nestle Cerelac Wheat Apple", "Nestle", "Infant cereal from 6 months.", 5, [v("300 g", 300, "G", 295, 285)]),
   ]),
 ]),
 ("FR", "Frozen & Ready to Cook", "फ्रोज़न और रेडी टू कुक", "#0277BD", [
   ("Frozen Vegetables", "फ्रोज़न सब्ज़ियाँ", [
     ("Safal Frozen Green Peas", "Safal", "Farm-fresh frozen peas.", 5, [v("500 g", 500, "G", 120, 105)]),
     ("Safal Mixed Vegetables", "Safal", "Frozen carrot, beans, peas and corn.", 5, [v("500 g", 500, "G", 130, 115)]),
   ]),
   ("Frozen Snacks", "फ्रोज़न स्नैक्स", [
     ("McCain French Fries", "McCain", "Crispy shoestring fries.", 12, [v("420 g", 420, "G", 145, 125)]),
     ("Haldiram's Malabar Paratha", "Haldiram's", "Frozen flaky parathas, 5 pcs.", 12, [v("400 g", 400, "G", 120, 110)]),
   ]),
   ("Noodles & Pasta", "नूडल्स और पास्ता", [
     ("Maggi 2-Minute Masala Noodles", "Maggi", "India's favourite masala noodles.", 12, [v("Pack of 4", 4, "PCS", 60, 58), v("Pack of 12", 12, "PCS", 180, 168)]),
     ("Del Monte Penne Pasta", "Del Monte", "Durum wheat penne.", 18, [v("500 g", 500, "G", 160, 135)]),
   ]),
   ("Ready to Eat", "रेडी टू ईट", [
     ("MTR Ready to Eat Dal Makhani", "MTR", "Heat and eat in 2 minutes.", 12, [v("300 g", 300, "G", 140, 125)]),
     ("Haldiram's Minute Khana Rajma Chawal", "Haldiram's", "Ready meal.", 12, [v("300 g", 300, "G", 120, 110)]),
   ]),
 ]),
 ("DF", "Dry Fruits", "सूखे मेवे", "#795548", [
   ("Nuts", "मेवे", [
     ("California Almonds", "Gola Select", "Premium whole almonds.", 5, [v("250 g", 250, "G", 299, 245), v("500 g", 500, "G", 580, 469)]),
     ("Whole Cashews W320", "Gola Select", "Premium W320 grade kaju.", 5, [v("250 g", 250, "G", 330, 279)]),
     ("Walnut Kernels", "Gola Select", "Light halves, fresh crop.", 5, [v("200 g", 200, "G", 399, 335)]),
     ("Salted Pistachios", "Gola Select", "Roasted and salted pista.", 5, [v("200 g", 200, "G", 399, 349)]),
   ]),
   ("Raisins & Dates", "किशमिश और खजूर", [
     ("Golden Raisins (Kishmish)", "Gola Select", "Seedless golden raisins.", 5, [v("250 g", 250, "G", 160, 129)]),
     ("Lion Seedless Dates", "Lion", "Soft seedless khajoor.", 5, [v("500 g", 500, "G", 199, 180)]),
   ]),
 ]),
 ("PJ", "Puja Essentials", "पूजा सामग्री", "#FF8F00", [
   ("Agarbatti & Dhoop", "अगरबत्ती और धूप", [
     ("Cycle Three in One Agarbatti", "Cycle", "Three fragrances in one pack.", 5, [v("Pack of 120 sticks", 120, "PCS", 130, 120)]),
     ("Mangaldeep Sandal Dhoop Cones", "Mangaldeep", "Sandalwood dhoop cones.", 5, [v("Pack of 40", 40, "PCS", 60, 55)]),
   ]),
   ("Puja Items", "पूजा सामग्री", [
     ("Pure Camphor (Kapur) Tablets", "Mangaldeep", "Pure camphor for aarti.", 18, [v("50 g", 50, "G", 120, 110)]),
     ("Cotton Wicks (Round Batti)", "Gola Select", "Ready-to-use cotton wicks.", 5, [v("Pack of 200", 200, "PCS", 50, 40)]),
     ("Clay Diyas", "Gola Select", "Handmade terracotta diyas for festivals.", 5, [v("Pack of 12", 12, "PCS", 99, 79)]),
   ]),
 ]),
 ("PT", "Pet Care", "पालतू देखभाल", "#5D4037", [
   ("Dog Food & Treats", "कुत्ते का भोजन", [
     ("Pedigree Adult Chicken & Vegetables", "Pedigree", "Complete dry dog food.", 18, [v("1.2 kg", 1.2, "KG", 380, 345), v("3 kg", 3, "KG", 880, 799)]),
     ("Pedigree Dentastix", "Pedigree", "Dental chew treats for medium dogs.", 18, [v("Pack of 7", 7, "PCS", 199, 185)]),
   ]),
   ("Cat Food", "बिल्ली का भोजन", [
     ("Whiskas Adult Ocean Fish", "Whiskas", "Dry cat food for adult cats.", 18, [v("1.2 kg", 1.2, "KG", 499, 455)]),
   ]),
   ("Pet Accessories", "पालतू सहायक सामान", [
     ("Stainless Steel Pet Bowl", "Gola Select", "Anti-skid steel bowl, medium.", 18, [v("1 pc", 1, "PCS", 299, 229)]),
   ]),
 ]),
 ("KH", "Kitchen & Household", "रसोई और घरेलू", "#455A64", [
   ("Kitchen Essentials", "रसोई आवश्यक", [
     ("Aluminium Foil 9 m", "Gola Select", "Food-grade aluminium foil.", 18, [v("1 roll", 1, "PCS", 120, 99)]),
     ("Airtight Food Containers Set", "Gola Select", "BPA-free containers, set of 3.", 18, [v("Set of 3", 3, "PCS", 399, 299)]),
     ("Disposable Paper Plates", "Gola Select", "Eco-friendly areca/paper plates, 10 inch.", 18, [v("Pack of 25", 25, "PCS", 150, 119)]),
   ]),
   ("Batteries & Lights", "बैटरी और लाइट", [
     ("Eveready AA Alkaline Batteries", "Eveready", "Long-lasting AA batteries.", 18, [v("Pack of 4", 4, "PCS", 140, 125)]),
     ("Philips 9W LED Bulb", "Philips", "Cool daylight B22 LED bulb.", 12, [v("1 pc", 1, "PCS", 140, 99)]),
   ]),
 ]),
 ("ST", "Stationery", "स्टेशनरी", "#283593", [
   ("Pens & Pencils", "पेन और पेंसिल", [
     ("Reynolds 045 Fine Carbure Ball Pen Blue", "Reynolds", "Smooth ball pens.", 18, [v("Pack of 10", 10, "PCS", 100, 90)]),
     ("Apsara Platinum Extra Dark Pencils", "Apsara", "Extra dark pencils with eraser and sharpener.", 12, [v("Pack of 10", 10, "PCS", 60, 55)]),
   ]),
   ("Notebooks & School Supplies", "नोटबुक और स्कूल सामग्री", [
     ("Classmate Single Line Notebook", "Classmate", "172 pages, 24 x 18 cm.", 12, [v("Pack of 6", 6, "PCS", 330, 290)]),
     ("Fevicol MR Squeeze Bottle", "Fevicol", "All-purpose white adhesive.", 18, [v("50 g", 50, "G", 30, 30)]),
   ]),
 ]),
]

BANNERS = [
  ("Fresh Fruits & Veggies", "Up to 30% off on farm-fresh produce", "CATEGORY", "fruits-vegetables", "#0C831F"),
  ("Monthly Kirana Stock-up", "Atta, rice, dal & oil at the best prices", "CATEGORY", "atta-rice-dal", "#F9A825"),
  ("Flat ₹75 off on your first order", "Use code GOLA75 on orders above ₹299", "COUPON", "GOLA75", "#1B5E20"),
  ("Festive Puja Essentials", "Diyas, agarbatti & more", "CATEGORY", "puja-essentials", "#FF8F00"),
]

COUPONS = [
  # code, desc, type, value, max_disc, min_order, first_order_only
  ("GOLA75", "Flat ₹75 off on your first order above ₹299", "FLAT", 75, None, 299, True),
  ("FRESH10", "10% off up to ₹100 on orders above ₹499", "PERCENT", 10, 100, 499, False),
  ("SAVE50", "Flat ₹50 off on orders above ₹699", "FLAT", 50, None, 699, False),
]

PINCODES = [
  ("201301", "Noida", "Sector 18", None, 20), ("201303", "Noida", "Sector 62", None, 25),
  ("201304", "Noida", "Sector 50", None, 25), ("110001", "New Delhi", "Connaught Place", None, 30),
  ("110017", "New Delhi", "Malviya Nagar", None, 30), ("122001", "Gurugram", "Old Gurgaon", 35, 35),
  ("122018", "Gurugram", "Sohna Road", 35, 35), ("400001", "Mumbai", "Fort", None, 30),
  ("400050", "Mumbai", "Bandra West", None, 25), ("560001", "Bengaluru", "MG Road", None, 30),
  ("560034", "Bengaluru", "Koramangala", None, 20), ("560102", "Bengaluru", "HSR Layout", None, 20),
]
