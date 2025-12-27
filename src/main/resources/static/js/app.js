document.addEventListener("DOMContentLoaded", () => {
    console.log("Admin panel loaded");

    // пример будущей логики
    document.querySelectorAll(".card").forEach(card => {
        card.addEventListener("mouseenter", () => {
            card.style.cursor = "pointer";
        });
    });
});