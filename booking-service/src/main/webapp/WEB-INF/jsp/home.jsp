<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Costco Travel Smart Rebook</title>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="/static/css/main.css">
</head>
<body>
<jsp:include page="fragments/header.jsp" />
<main>
  <div class="search-band">
  <div class="page-wrap">
    <div class="search-module">
      <div class="search-tabs" role="tablist" aria-label="Search type">
        <button type="button" class="active" data-tab="packages"><span class="tab-icon">&#128188;</span> Packages</button>
        <button type="button" data-tab="hotels"><span class="tab-icon">&#127976;</span> Hotels</button>
        <button type="button" data-tab="cruises"><span class="tab-icon">&#128674;</span> Cruises</button>
        <button type="button" data-tab="cars"><span class="tab-icon">&#128663;</span> Rental Cars</button>
      </div>

      <form class="search-form" id="packages-form" data-tabpanel="packages">
        <div class="field">
          <label for="pk-dest">Destination</label>
          <input type="text" id="pk-dest" name="destination" placeholder="e.g. Maui, Hawaii">
        </div>
        <div class="field">
          <label for="pk-depart">Depart</label>
          <input type="date" id="pk-depart" name="depart">
        </div>
        <div class="field">
          <label for="pk-return">Return</label>
          <input type="date" id="pk-return" name="return">
        </div>
        <div class="field">
          <label for="pk-guests">Guests</label>
          <select id="pk-guests" name="guests">
            <option>2 Adults</option>
            <option>2 Adults, 2 Children</option>
            <option>1 Adult</option>
          </select>
        </div>
        <div class="field"><a class="btn btn-primary" href="/not-included">Search</a></div>
      </form>

      <form class="search-form" id="hotels-form" data-tabpanel="hotels" hidden>
        <div class="field">
          <label for="ht-dest">City or Hotel</label>
          <input type="text" id="ht-dest" name="destination" placeholder="e.g. Wailea Beach Resort">
        </div>
        <div class="field">
          <label for="ht-checkin">Check-in</label>
          <input type="date" id="ht-checkin" name="checkin">
        </div>
        <div class="field">
          <label for="ht-checkout">Check-out</label>
          <input type="date" id="ht-checkout" name="checkout">
        </div>
        <div class="field"><a class="btn btn-primary" href="/not-included">Search</a></div>
      </form>

      <form class="search-form" id="cruises-form" data-tabpanel="cruises" hidden>
        <div class="field">
          <label for="cr-dest">Departure Port</label>
          <input type="text" id="cr-dest" name="port" placeholder="e.g. Honolulu, HI">
        </div>
        <div class="field">
          <label for="cr-date">Sail Date</label>
          <input type="date" id="cr-date" name="saildate">
        </div>
        <div class="field"><a class="btn btn-primary" href="/not-included">Search</a></div>
      </form>

      <form class="search-form" id="cars-form" data-tabpanel="cars" hidden>
        <div class="field">
          <label for="ca-loc">Pick-up Location</label>
          <input type="text" id="ca-loc" name="location" placeholder="e.g. Kahului Airport (OGG)">
        </div>
        <div class="field">
          <label for="ca-pickup">Pick-up Date</label>
          <input type="date" id="ca-pickup" name="pickup">
        </div>
        <div class="field">
          <label for="ca-dropoff">Drop-off Date</label>
          <input type="date" id="ca-dropoff" name="dropoff">
        </div>
        <div class="field"><a class="btn btn-primary" href="/not-included">Search</a></div>
      </form>
    </div>
  </div>
  </div>

  <div class="page-wrap">
    <section class="promo-card">
      <div class="promo-media" aria-hidden="true"></div>
      <div class="promo-body">
        <span class="promo-badge">Hawaii</span>
        <h2>Maui Member Value</h2>
        <p>Your upcoming Wailea Beach Resort escape is already booked and ready for March 2027.</p>
        <p class="promo-check">$200 Digital Costco Shop Card</p>
        <p class="promo-check">Standard SUV rental included</p>
        <a class="btn btn-primary" href="/account/bookings/CT-DEMO-78291">View Upcoming Trip</a>
      </div>
    </section>

    <section class="home-section">
      <h2 class="home-section-title">What&rsquo;s Hot</h2>
      <div class="card-grid">
        <div class="home-card">
          <div class="card-thumb thumb-sand"><span class="card-thumb-glyph">M</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Limited-Time Offer</span>
            <h3>Maui, Hawaii</h3>
            <p>All-inclusive resort package with member savings on select travel dates.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-ocean"><span class="card-thumb-glyph">L</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Beachfront Deal</span>
            <h3>Los Cabos, Mexico</h3>
            <p>Oceanfront resort stay with a bundled airfare and shuttle option.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-sunset"><span class="card-thumb-glyph">O</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Family Favorite</span>
            <h3>Orlando, Florida</h3>
            <p>Theme park bundle pairing nearby hotels with multi-day admission.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-forest"><span class="card-thumb-glyph">A</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Member Exclusive</span>
            <h3>Alaska</h3>
            <p>Seven-night cruise and land package through glacier country.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-plum"><span class="card-thumb-glyph">C</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Adults Only</span>
            <h3>Cancun, Mexico</h3>
            <p>Adults-only resort deal with a shop card credit for early booking.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-slate"><span class="card-thumb-glyph">V</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Weekend Getaway</span>
            <h3>Las Vegas, Nevada</h3>
            <p>Strip hotel and show package with flexible cancellation.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
      </div>
    </section>

    <section class="home-section">
      <h2 class="home-section-title">Featured Travel</h2>
      <div class="card-grid">
        <div class="home-card">
          <div class="card-thumb thumb-ocean"><span class="card-thumb-glyph">P</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Guided Tour</span>
            <h3>Portugal</h3>
            <p>Coastal cities and countryside on an escorted multi-city itinerary.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-forest"><span class="card-thumb-glyph">C</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Nature &amp; Beach</span>
            <h3>Costa Rica</h3>
            <p>Rainforest lodges paired with a beachfront resort stay.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-slate"><span class="card-thumb-glyph">A</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Ski Season</span>
            <h3>Aspen, Colorado</h3>
            <p>Mountain resort getaway with lift-ticket bundle options.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-sunset"><span class="card-thumb-glyph">N</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">City Break</span>
            <h3>New York City</h3>
            <p>Manhattan hotel stay bundled with a Broadway show package.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-sand"><span class="card-thumb-glyph">W</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">History &amp; Culture</span>
            <h3>Washington, D.C.</h3>
            <p>Monuments and museums stay near the National Mall.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-plum"><span class="card-thumb-glyph">S</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">National Parks</span>
            <h3>Salt Lake City, Utah</h3>
            <p>A basecamp hotel stay for exploring nearby national parks.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
      </div>
    </section>

    <section class="home-section">
      <h2 class="home-section-title">Featured Cruises</h2>
      <div class="card-grid">
        <div class="home-card">
          <div class="card-thumb thumb-ocean"><span class="card-thumb-glyph">C</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Western Loop</span>
            <h3>Caribbean</h3>
            <p>Seven-night round-trip sailing with stops across three islands.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-sand"><span class="card-thumb-glyph">M</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Ports of Call</span>
            <h3>Mediterranean</h3>
            <p>Ten-night itinerary visiting historic ports across southern Europe.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-slate"><span class="card-thumb-glyph">A</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Glacier Route</span>
            <h3>Alaska Inside Passage</h3>
            <p>Seven-night scenic sailing through coastal glacier country.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
      </div>
    </section>

    <section class="home-section">
      <h2 class="home-section-title">Featured Destinations</h2>
      <div class="card-grid">
        <div class="home-card">
          <div class="card-thumb thumb-ocean"><span class="card-thumb-glyph">S</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Coastal City</span>
            <h3>San Diego, California</h3>
            <p>Waterfront hotels within reach of the zoo and downtown harbor.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-forest"><span class="card-thumb-glyph">P</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Pacific Northwest</span>
            <h3>Portland, Oregon</h3>
            <p>Downtown stays close to gardens, breweries, and river trails.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-slate"><span class="card-thumb-glyph">C</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Midwest Icon</span>
            <h3>Chicago, Illinois</h3>
            <p>Downtown loop hotels near the riverwalk and museum campus.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
      </div>
    </section>

    <section class="home-section">
      <h2 class="home-section-title">Featured Guided Vacations &amp; Tours</h2>
      <div class="card-grid">
        <div class="home-card">
          <div class="card-thumb thumb-sunset"><span class="card-thumb-glyph">I</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Classic Cities</span>
            <h3>Italy</h3>
            <p>An escorted tour connecting the country&rsquo;s classic city stops.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-forest"><span class="card-thumb-glyph">C</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Adventure Tour</span>
            <h3>Costa Rica</h3>
            <p>Small-group tour combining rainforest hikes and beach time.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-sand"><span class="card-thumb-glyph">N</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Southwest Loop</span>
            <h3>National Parks Tour</h3>
            <p>A guided loop through the Southwest&rsquo;s best-known parks.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
      </div>
    </section>

    <section class="home-section">
      <h2 class="home-section-title">Maximize Your Rewards</h2>
      <div class="card-grid">
        <div class="home-card">
          <div class="card-thumb thumb-plum"><span class="card-thumb-glyph">E</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Executive Members</span>
            <h3>Extra Travel Credit</h3>
            <p>Executive Members can earn additional credit back on eligible bookings.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-ocean"><span class="card-thumb-glyph">S</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Shop Card Bonus</span>
            <h3>Digital Shop Card Offers</h3>
            <p>Select packages include a digital Costco Shop Card at booking.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-sunset"><span class="card-thumb-glyph">R</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Card Benefits</span>
            <h3>Travel Card Rewards</h3>
            <p>Combine card rewards with member pricing for extra travel value.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
      </div>
    </section>

    <section class="home-section">
      <h2 class="home-section-title">Explore More Travel</h2>
      <div class="card-grid">
        <div class="home-card">
          <div class="card-thumb thumb-sand"><span class="card-thumb-glyph">V</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Packages</span>
            <h3>Vacation Packages</h3>
            <p>Bundle a hotel, flight, and rental car into a single itinerary.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-slate"><span class="card-thumb-glyph">R</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Rental Cars</span>
            <h3>Rental Cars</h3>
            <p>Compare rental car options at airports and city locations.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
        <div class="home-card">
          <div class="card-thumb thumb-forest"><span class="card-thumb-glyph">T</span></div>
          <div class="home-card-body">
            <span class="home-card-tag">Peace of Mind</span>
            <h3>Travel Insurance &amp; Protection</h3>
            <p>Add trip protection to safeguard your booking against the unexpected.</p>
            <a class="home-card-link" href="/not-included">Learn more &rsaquo;</a>
          </div>
        </div>
      </div>
    </section>
  </div>
</main>
<jsp:include page="fragments/footer.jsp" />
<script>
  document.querySelectorAll('.search-tabs button').forEach(function (btn) {
    btn.addEventListener('click', function () {
      document.querySelectorAll('.search-tabs button').forEach(function (b) { b.classList.remove('active'); });
      document.querySelectorAll('.search-form').forEach(function (f) { f.hidden = true; });
      btn.classList.add('active');
      var panel = document.getElementById(btn.dataset.tab + '-form');
      if (panel) { panel.hidden = false; }
    });
  });
</script>
</body>
</html>
