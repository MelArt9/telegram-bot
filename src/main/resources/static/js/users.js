function deleteUser(id) {
    if (!confirm("Удалить пользователя?")) return;

    fetch(`/users/${id}`, {
        method: "DELETE"
    })
        .then(res => {
            if (!res.ok) throw new Error();
            location.reload();
        })
        .catch(() => alert("Ошибка при удалении пользователя"));
}