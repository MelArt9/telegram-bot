document.querySelectorAll(".card").forEach(card => {
    card.addEventListener("click", () => {
        card.classList.add("active");
        setTimeout(() => card.classList.remove("active"), 200);
    });
});