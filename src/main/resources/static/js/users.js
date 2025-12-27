function deleteUser(id) {
    if (!confirm("Удалить пользователя?")) return;

    fetch(`/api/users/${id}`, {
        method: "DELETE"
    }).then(res => {
        if (res.ok) {
            location.reload();
        } else {
            alert("Ошибка удаления пользователя");
        }
    });
}